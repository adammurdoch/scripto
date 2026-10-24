package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.file.PosixPermissions
import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.*
import net.rubygrapefruit.scripto.ir.*
import kotlin.math.exp

class BashGenerator {
    private val listToString by lazy {
        BashGeneratedFunction(
            "_list_to_string", "_LIST_TO_STRING", $$"""
local RESULT="["                
local SEPARATOR=""
local ARRAY_REF="$1[@]"
for ELEMENT in "${!ARRAY_REF}"; do
    RESULT+="$SEPARATOR$ELEMENT"
    SEPARATOR=", "
done
RESULT+="]"
_LIST_TO_STRING="$RESULT"
            """.trimIndent()
        )
    }

    fun generate(program: Program, outputFile: RegularFile) {
        program.visitExpressions { expression ->
            if (expression is FunctionCallExpression) {
                val target = expression.target
                if (target is RealizedFunctionDefinition && target.definition == StandardLibrary.listConstructor) {
                    if (target.argument is Type.List) {
                        outputFile.delete()
                        throw IllegalArgumentException("${expression.target.returnType} is not supported for Bash")
                    }
                }
            }
        }

        val scope = StatementScope()

        val functionMapper = createFunctionMapper(scope)
        val operationMapper = createOperationMapper(functionMapper)

        val statements = operationMapper.map(program.operations)

        val queue = Declaration.Queue<BashGeneratedFunction>()
        for (statement in statements) {
            statement.visitExpressions { expression ->
                expression.visitDeclarations { declaration ->
                    queue.add(declaration)
                }
            }
        }

        writeTo(outputFile, "#") {
            queue.visitAll { function ->
                append(function)
            }
            for (statement in statements) {
                append(statement)
            }
        }
        outputFile.setPermissions(PosixPermissions.readWriteFile.executable())
    }

    private fun createOperationMapper(functionMapper: FunctionMapper<BashExpression>): OperationMapper<BashStatement> {
        return OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                BashCommandInvocation("echo", emptyList())
            }
            map(StandardLibrary.printlnOneArg) { call ->
                val call = StandardLibrary.convertPrintln(call)
                val args = call.arguments.map { toExpression(it, functionMapper) }
                BashCommandInvocation("echo", args)
            }
            map { declaration ->
                val value = declaration.value
                BashVariableAssignment(declaration.name, toExpression(value, functionMapper))
            }
            collectDependencies { statement: BashStatement ->
                addDependencies(statement)
            }
        }
    }

    private fun createFunctionMapper(scope: StatementScope): FunctionMapper<BashExpression> {
        return FunctionMapper.of {
            map(StandardLibrary.fileConstructor) { expression, functionMapper ->
                toExpression(expression.args.first(), functionMapper)
            }
            map(StandardLibrary.listConstructor) { expression, _, functionMapper ->
                BashArrayLiteral(expression.args.map { toExpression(it, functionMapper) })
            }
            mapOperator(StandardLibrary.intAddition) { left, right ->
                BashArithmetic(left, "+", right)
            }
            mapOperator(StandardLibrary.stringConcatenation) { left, right ->
                BashConcatenatedExpressions(listOf(left, right))
            }

            val equality = { left: BashExpression, right: BashExpression ->
                val test = listOf(left, BashWord("="), right)
                val trueBranch = BashVariableAssignment("TMP_VAR", BashWord("true"))
                val falseBranch = BashVariableAssignment("TMP_VAR", BashWord("false"))
                val statement = BashIfStatement(test, trueBranch, falseBranch)
                BashCommandInvocationExpression("TMP_VAR", listOf(statement))
            }
            mapOperator(StandardLibrary.stringEquality, equality)
            mapOperator(StandardLibrary.intEquality, equality)
            mapOperator(StandardLibrary.booleanEquality, equality)

            mapReceiver(StringFunctions.toString) { expression, functionMapper ->
                toExpression(expression.receiver, functionMapper)
            }
            mapReceiver(IntFunctions.toString) { expression, functionMapper ->
                toExpression(expression.receiver, functionMapper)
            }
            mapReceiver(BooleanFunctions.toString) { expression, functionMapper ->
                toExpression(expression.receiver, functionMapper)
            }
            mapReceiver(FileFunctions.toString) { expression, functionMapper ->
                BashConcatenatedExpressions(listOf(BashString("File("), toExpression(expression.receiver, functionMapper), BashString(")")))
            }
            mapReceiver(ListFunctions.toString) { expression, _ ->
                val receiver = expression.receiver
                when (receiver) {
                    is VariableReference -> {
                        BashCommandInvocationExpression(
                            listToString.returnVar, listOf(
                                BashCommandInvocation(listToString.name, listOf(BashWord(receiver.target.name)))
                            ),
                            listToString
                        )
                    }

                    else -> BashString("??")
                }
            }
            mapReceiver(FileFunctions.readText) { expression, functionMapper ->
                val varName = scope.newVarName("READ_TEXT")
                val readText = readFileIntoVar(toExpression(expression.receiver, functionMapper), varName)
                BashCommandInvocationExpression(varName, listOf(readText))
            }
        }
    }

    private fun FunctionMapper.Builder<BashExpression>.mapOperator(operator: OperatorDefinition, builder: (BashExpression, BashExpression) -> BashExpression) {
        map(operator) { expression, functionMapper ->
            val left = toExpression(expression.args[0], functionMapper)
            val right = toExpression(expression.args[1], functionMapper)
            builder(left, right)
        }
    }

    private fun toExpression(expression: Expression, functionMapper: FunctionMapper<BashExpression>): BashExpression {
        return when (expression) {
            is StringLiteral -> BashString(expression.value)
            is IntLiteral -> BashWord(expression.value.toString())
            is BooleanLiteral -> BashWord(expression.value.toString())
            is VariableReference -> varReferenceToBash(expression)
            is FunctionCallExpression -> functionMapper.map(expression)
            is ReceiverFunctionCallExpression -> functionMapper.map(expression)
        }
    }

    private fun varReferenceToBash(expression: VariableReference): BashWord {
        return when (expression.type) {
            is Type.String, is Type.File, is Type.List -> BashWord("\"\$${expression.target.name}\"")
            else -> BashWord("\$${expression.target.name}")
        }
    }

    private fun readFileIntoVar(file: BashExpression, varName: String): BashCommandInvocation {
        return BashCommandInvocation("IFS= read -rd '' $varName", emptyList(), redirectInput = file)
    }

    private fun SourceGenerator.append(function: BashGeneratedFunction) {
        append("function ")
        append(function.name)
        appendln(" {")
        for (line in function.body.lines()) {
            append("    ")
            appendln(line)
        }
        appendln("}")
        appendln()
    }

    private fun SourceGenerator.append(statement: BashStatement) {
        when (statement) {
            is BashVariableAssignment -> append(statement)
            is BashVariableValueAppend -> append(statement)
            is BashCommandInvocation -> append(statement)
            is BashIfStatement -> append(statement)
            is BashForLoop -> append(statement)
        }
    }

    private fun SourceGenerator.append(statement: BashVariableAssignment) {
        append(statement.name)
        append("=")
        append(statement.value)
        appendln()
    }

    private fun SourceGenerator.append(statement: BashVariableValueAppend) {
        append(statement.name)
        append("+=")
        append(statement.value)
        appendln()
    }

    private fun SourceGenerator.append(statement: BashCommandInvocation) {
        append(statement.command)
        for (arg in statement.arguments) {
            append(" ")
            append(arg)
        }
        if (statement.redirectInput != null) {
            append("<")
            append(statement.redirectInput)
        }
        appendln()
    }

    private fun SourceGenerator.append(statement: BashIfStatement) {
        append("if [ ")
        for (arg in statement.test) {
            append(arg)
            append(' ')
        }
        appendln("]; then")
        append(statement.trueBranch)
        appendln("else")
        append(statement.falseBranch)
        appendln("fi")
    }

    private fun SourceGenerator.append(statement: BashForLoop) {
        append("for ")
        append(statement.elementVarName)
        append(" in ")
        append(statement.elements)
        append("; do\n")
        for (loopStatement in statement.statements) {
            append(loopStatement)
        }
        append("done\n")
    }

    private fun SourceGenerator.append(expression: BashExpression) {
        when (expression) {
            is BashString -> {
                append("\"")
                for (ch in expression.value) {
                    when (ch) {
                        '`', '$', '\\' -> {
                            append("\\")
                            append(ch)
                        }

                        else -> append(ch)
                    }
                }
                append("\"")
            }

            is BashWord -> append(expression.value)

            is BashConcatenatedExpressions -> {
                for (nested in expression.values) {
                    append(nested)
                }
            }

            is BashArithmetic -> {
                append("$((")
                append(expression.left)
                append(expression.operator)
                append(expression.right)
                append("))")
            }

            is BashArrayLiteral -> {
                append("(")
                append(expression.elements, " ") { element ->
                    append(element)
                }
                append(")")
            }

            is BashCommandInvocationExpression -> {
                append("\"$")
                append(expression.intermediateVariable)
                append("\"")
            }
        }
    }
}