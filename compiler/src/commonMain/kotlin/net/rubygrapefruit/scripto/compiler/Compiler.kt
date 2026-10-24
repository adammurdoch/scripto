package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.parse.ParseException
import net.rubygrapefruit.parse.Parser
import net.rubygrapefruit.parse.Position
import net.rubygrapefruit.parse.combinators.*
import net.rubygrapefruit.parse.general.endOfInput
import net.rubygrapefruit.parse.general.position
import net.rubygrapefruit.parse.text.*
import net.rubygrapefruit.scripto.ir.*

class Compiler {
    private val parser = createParser()

    fun compile(sourceFile: RegularFile): Result {
        val result = doCompile(sourceFile)
        if (result is Result.Failure) {
            for (error in result.errors) {
                println(error.message)
            }
        }
        return result
    }

    private fun doCompile(sourceFile: RegularFile): Result {
        val sourceText = sourceFile.readText()
        val parseResult = parser.parse(sourceText)
        val trees = try {
            parseResult.get()
        } catch (e: ParseException) {
            return Result.Failure(listOf(Error(e.message!!)))
        }

        val resolveResult = ProgramTree(trees).resolve(StandardLibrary)
        return when (resolveResult) {
            is NotResolved -> {
                val errors = resolveResult.problems.distinct().map { problem ->
                    if (problem.position != null) {
                        addContext(problem.position, problem, sourceText)
                    } else {
                        Error(problem.message)
                    }
                }
                Result.Failure(errors)
            }

            is Resolved -> Result.Success(resolveResult.result)
        }
    }

    private fun addContext(position: Position, problem: Problem, sourceText: String): Error {
        var line = 1
        var col = 1
        for (pos in 0 until position.value) {
            if (sourceText[pos] == '\n') {
                line++
                col = 1
            } else {
                col++
            }
        }
        return Error("Line ${line}: $col: ${problem.message}")
    }

    // Functions to move to Kotlin tools

    fun <IN> discardSequence(vararg parsers: Parser<IN, *>): Parser<IN, Unit> {
        return sequence(parsers.map { discard(it) })
    }

    fun <IN, A, B, OUT> surrounded(prefix: Parser<IN, *>, a: Parser<IN, A>, b: Parser<IN, B>, suffix: Parser<IN, *>, map: (A, B) -> OUT): Parser<IN, OUT> {
        return surrounded(prefix, sequence(a, b, map), suffix)
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
        val string = surrounded(quote, stringBody, quote) { literal -> StringLiteral(literal) }
//        val stringLiteral = surrounded(quote, position(), stringBody, quote) { position, literal -> LiteralTree(position, StringLiteral(literal)) }

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

        val int = map(oneOf(zero, negativeNumber, number)) { literal -> IntLiteral(literal) }

        val booleanTrue = literal("true", BooleanLiteral(true))
        val booleanFalse = literal("false", BooleanLiteral(false))

        val variableReference = sequence(position(), identifier) { pos, id -> VariableReferenceTree(pos, id) }

        val expression = recursive<TextInput, ExpressionTree>()

        val argSeparator = sequence(optionalWhitespace, literal(','), optionalWhitespace)
        val argsPrefix = sequence(literal('('), optionalWhitespace)
        val argsSuffix = sequence(optionalWhitespace, literal(')'))
        val argsList = surrounded(argsPrefix, zeroOrMore(expression, separator = argSeparator), argsSuffix)

        val listLiteralPrefix = sequence(literal('['), optionalWhitespace)
        val listLiteralSuffix = sequence(optionalWhitespace, literal(']'))
        val listLiteral = surrounded(listLiteralPrefix, position(), zeroOrMore(expression, separator = argSeparator), listLiteralSuffix) { position, elements ->
            ListLiteralTree(position, elements)
        }

        val functionCall = sequence(position(), identifier, argsList) { pos, name, args -> FunctionCallTree(pos, name, args) }

        val literal = sequence(position(), oneOf(string, int, booleanTrue, booleanFalse)) { position, literal -> LiteralTree(position, literal) }
        val functionReceiver = oneOf(literal, functionCall, variableReference, listLiteral)

        val receiverSeparator = sequence(optionalWhitespace, literal('.'), optionalWhitespace)
        val receiverFunctionCall = sequence(functionReceiver, zeroOrMore(prefixed(receiverSeparator, functionCall))) { receiver, calls ->
            calls.fold(receiver) { r, c -> ReceiverFunctionCallTree(r, c) }
        }

        val infixArgument = receiverFunctionCall

        val operator = oneOf(
            literal("+", Operator.Plus),
            literal("==", Operator.Equality),
        )
        val infixOperator = surrounded(optionalWhitespace, operator, optionalWhitespace)
        val infix = sequence(position(), infixOperator, infixArgument) { position, op, arg -> OperatorSuffix(position, op, arg) }
        val infixFunctionCall = sequence(infixArgument, zeroOrMore(infix)) { first, calls ->
            calls.fold(first) { left, op -> InfixOperatorTree(left, op.position, op.operator, op.right) }
        }

        expression.parser(infixFunctionCall)

        val assignmentSeparator = sequence(optionalWhitespace, literal("="), optionalWhitespace)
        val variableDeclarationPrefix = sequence(literal("val"), whitespace)
        val variableDeclaration =
            sequence(variableDeclarationPrefix, position(), identifier, assignmentSeparator, expression) { _, pos, name, _, value -> VariableDeclarationTree(pos, name, value) }

        val lineTerminator = oneOf(endOfLine(), endOfInput())
        val comment = discardSequence(literal("//"), zeroOrMore(oneExcept(lineTerminator)))
        val blankLine = sequence(optionalWhitespace, optional(comment), lineTerminator)
        val blankLines = zeroOrMore(blankLine)

        val statement = suffixed(oneOf(functionCall, variableDeclaration), blankLine)

        return surrounded(blankLines, zeroOrMore(statement, separator = blankLines), blankLines)
    }

    private class OperatorSuffix(val position: Position, val operator: Operator, val right: ExpressionTree)

    sealed interface Result {
        fun get(): Program

        class Success(val program: Program) : Result {
            override fun get(): Program {
                return program
            }
        }

        class Failure(val errors: List<Error>) : Result {
            override fun get(): Program {
                throw if (errors.size == 1) {
                    IllegalArgumentException("Compile failed with 1 error")
                } else {
                    IllegalArgumentException("Compile failed with ${errors.size} errors")
                }
            }
        }
    }

    class Error(val message: String)
}