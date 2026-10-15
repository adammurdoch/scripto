package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.parse.ParseException
import net.rubygrapefruit.parse.Parser
import net.rubygrapefruit.parse.combinators.*
import net.rubygrapefruit.parse.general.endOfInput
import net.rubygrapefruit.parse.general.position
import net.rubygrapefruit.parse.text.*
import net.rubygrapefruit.parse.text.file.parse
import net.rubygrapefruit.scripto.ir.*

class Compiler {
    private val parser = createParser()

    fun compile(sourceFile: RegularFile): Result {

        val parseResult = parser.parse(sourceFile)
        val trees = try {
            parseResult.get()
        } catch (e: ParseException) {
            return Result.Failure(listOf(Problem(null, e.message!!)))
        }

        val library = StandardLibrary()
        val resolveResult = ProgramTree(trees).resolve(library)
        return when (resolveResult) {
            is NotResolved -> {
                for (problem in resolveResult.problems) {
                    if (problem.position != null) {
                        println("${problem.position}: ${problem.message}")
                    } else {
                        println(problem.message)
                    }
                }
                Result.Failure(resolveResult.problems)
            }

            is Resolved -> Result.Success(resolveResult.result)
        }
    }

    // Functions to move to Kotlin tools

    fun <IN> discardSequence(vararg parsers: Parser<IN, *>): Parser<IN, Unit> {
        return sequence(parsers.map { discard(it) })
    }

    private fun createParser(): Parser<TextInput, List<OperationTree>> {
        val whitespaceChar = oneOf(' ', '\t')
        val optionalWhitespace = discard(zeroOrMore(whitespaceChar))
        val whitespace = discard(oneOrMore(whitespaceChar))

        val initialChar = oneOf(oneInRange('a'..'z'), oneInRange('A'..'Z'))
        val subsequentChar = oneOf(initialChar, digit(), literal("_", '_'))
        val identifier = describedAs(match(sequence(discard(initialChar), zeroOrMore(discard(subsequentChar)))), "an identifier")

        val quote = literal('"')
        val stringBody = match(zeroOrMore(oneExcept(oneOf(endOfLine(), quote))))
        val stringLiteral = surrounded(quote, stringBody, quote) { LiteralTree(StringLiteral(it)) }

        val zero = literal("0", 0)
        val firstDigit = discard(oneInRange('1'..'9'))
        val subsequentDigit = discard(oneInRange('0'..'9'))
        val number = check(match(sequence(firstDigit, zeroOrMore(subsequentDigit)))) {
            try {
                MappingResult.of(it.toInt())
            } catch (_: NumberFormatException) {
                MappingResult.expected("an integer")
            }
        }
        val negativeNumber = map(match(sequence(literal("-"), firstDigit, zeroOrMore(subsequentDigit)))) { it.toInt() }

        val intLiteral = map(oneOf(zero, negativeNumber, number)) { LiteralTree(IntLiteral(it)) }

        val trueLiteral = literal("true", LiteralTree(BooleanLiteral(true)))
        val falseLiteral = literal("false", LiteralTree(BooleanLiteral(false)))

        val variableReference = sequence(position(), identifier) { pos, id -> VariableReferenceTree(pos, id) }

        val additionArgument = oneOf(intLiteral, stringLiteral, trueLiteral, falseLiteral, variableReference)

        val operator = oneOf(
            literal("+", "+"),
            literal("==", "=="),
        )
        val infixOperator = surrounded(optionalWhitespace, operator, optionalWhitespace)
        val infixFunctionCall = sequence(additionArgument, infixOperator, additionArgument) { left, operator, right -> InfixOperatorTree(left, operator, right) }

        val expression = recursive<TextInput, ExpressionTree>()

        val argSeparator = sequence(optionalWhitespace, literal(","), optionalWhitespace)
        val argsPrefix = sequence(literal("("), optionalWhitespace)
        val argsSuffix = sequence(optionalWhitespace, literal(")"))
        val argsList = surrounded(argsPrefix, zeroOrMore(expression, separator = argSeparator), argsSuffix)

        val functionCall = sequence(identifier, argsList) { name, args -> FunctionCallTree(name, args) }
        val receiverFunctionCall = sequence(additionArgument, oneOrMore(prefixed(literal('.'), functionCall))) { receiver, calls ->
            calls.fold(receiver) { r, c -> ReceiverFunctionCallTree(r, c) }
        }

        expression.parser(oneOf(infixFunctionCall, receiverFunctionCall, intLiteral, stringLiteral, trueLiteral, falseLiteral, functionCall, variableReference))

        val assignmentSeparator = sequence(optionalWhitespace, literal("="), optionalWhitespace)
        val variableDeclarationPrefix = sequence(literal("val"), whitespace)
        val variableDeclaration = sequence(variableDeclarationPrefix, identifier, assignmentSeparator, expression) { _, name, _, value -> VariableDeclarationTree(name, value) }

        val lineTerminator = oneOf(endOfLine(), endOfInput())
        val comment = discardSequence(literal("//"), zeroOrMore(oneExcept(lineTerminator)))
        val blankLine = sequence(optionalWhitespace, optional(comment), lineTerminator)
        val blankLines = zeroOrMore(blankLine)

        val statement = suffixed(oneOf(functionCall, variableDeclaration), blankLine)

        return surrounded(blankLines, zeroOrMore(statement, separator = blankLines), blankLines)
    }

    sealed interface Result {
        fun get(): Program

        class Success(val program: Program) : Result {
            override fun get(): Program {
                return program
            }
        }

        class Failure(val problems: List<Problem>) : Result {
            override fun get(): Program {
                throw if (problems.size == 1) {
                    IllegalArgumentException("Compile failed with 1 error")
                } else {
                    IllegalArgumentException("Compile failed with ${problems.size} errors")
                }
            }
        }
    }
}