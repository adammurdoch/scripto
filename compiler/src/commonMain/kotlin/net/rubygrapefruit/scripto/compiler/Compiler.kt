package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.parse.Parser
import net.rubygrapefruit.parse.combinators.*
import net.rubygrapefruit.parse.general.endOfInput
import net.rubygrapefruit.parse.text.*
import net.rubygrapefruit.parse.text.file.parse
import net.rubygrapefruit.scripto.ir.*

class Compiler {
    private val parser = createCompiler()

    fun compile(sourceFile: RegularFile): Program {

        val trees = parser.parse(sourceFile).get()

        val library = StandardLibrary()
        val problems = mutableListOf<String>()
        val program = ProgramTree(trees).resolve(library, problems)
        if (problems.isNotEmpty()) {
            throw IllegalArgumentException("Compile failed: ${problems.joinToString(", ")}")
        }

        return program
    }

    // Functions to move to Kotlin tools

    fun <IN> discardSequence(vararg parsers: Parser<IN, *>): Parser<IN, Unit> {
        return sequence(parsers.map { discard(it) })
    }

    private fun createCompiler(): Parser<TextInput, List<OperationTree>> {
        val whitespaceChar = oneOf(' ', '\t')
        val optionalWhitespace = discard(zeroOrMore(whitespaceChar))
        val whitespace = discard(oneOrMore(whitespaceChar))

        val initialChar = oneInRange('a'..'z')
        val subsequentChar = oneOf(initialChar, oneInRange('A'..'Z'), digit(), literal("_", '_'))
        val identifier = match(sequence(discard(initialChar), zeroOrMore(discard(subsequentChar))))

        val quote = literal('"')
        val stringBody = match(zeroOrMore(oneExcept(oneOf(endOfLine(), quote))))
        val stringLiteral = surrounded(quote, stringBody, quote) { LiteralTree(StringLiteral(it)) }

        val zero = literal("0", 0)
        val firstDigit = discard(oneInRange('1'..'9'))
        val subsequentDigit = discard(oneInRange('0'..'9'))
        val number = map(match(sequence(firstDigit, zeroOrMore(subsequentDigit)))) { it.toInt() }
        val negativeNumber = map(match(sequence(literal("-"), firstDigit, zeroOrMore(subsequentDigit)))) { it.toInt() }

        val intLiteral = map(oneOf(zero, negativeNumber, number)) { LiteralTree(IntLiteral(it)) }

        val trueLiteral = literal("true", LiteralTree(BooleanLiteral(true)))
        val falseLiteral = literal("false", LiteralTree(BooleanLiteral(false)))

        val variableReference = map(identifier) { VariableReferenceTree(it) }

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

        expression.parser(oneOf(infixFunctionCall, intLiteral, stringLiteral, trueLiteral, falseLiteral, functionCall, variableReference))

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
}