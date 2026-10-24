package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.*
import net.rubygrapefruit.scripto.ir.*

class PowershellGenerator {
    fun generate(program: Program, outputFile: RegularFile) {
        val functionMapper = createFunctionMapper()
        val operationMapper = createOperationMapper(functionMapper)

        val statements = operationMapper.map(program.operations)
        val powershellProgram = PsProgram(statements)

        writeTo(outputFile, "#") {
            for (statement in powershellProgram.statements) {
                append(statement)
            }
        }
    }

    private fun createOperationMapper(functionMapper: FunctionMapper<PsExpression>): OperationMapper<PsStatement> {
        return OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                PsCommandInvocation("Write-Output", listOf(PsString("")))
            }
            map(StandardLibrary.printlnOneArg) { call ->
                val args = call.arguments.map { toStringExpression(it, functionMapper) }
                PsCommandInvocation("Write-Output", args)
            }
            map { declaration ->
                val value = declaration.value
                val initializer = toExpression(value, functionMapper)
                PsVariableDeclaration(declaration, initializer)
            }
        }
    }

    private fun createFunctionMapper(): FunctionMapper<PsExpression> {
        return FunctionMapper.of {
            map(StandardLibrary.fileConstructor) { expression, functionMapper ->
                toExpression(expression.args.first(), functionMapper)
            }
            map(StandardLibrary.listConstructor) { expression, _, functionMapper ->
                PsArrayOperator(expression.args.map { toExpression(it, functionMapper) })
            }
            mapOperator(StandardLibrary.stringEquality) { left, right ->
                PsInfixOperation(left, "-ceq", right)
            }
            mapOperator(StandardLibrary.stringConcatenation) { left, right ->
                PsInfixOperation(left, "+", right)
            }
            mapOperator(StandardLibrary.intEquality) { left, right ->
                PsInfixOperation(left, "-ceq", right)
            }
            mapOperator(StandardLibrary.intAddition) { left, right ->
                PsInfixOperation(left, "+", right)
            }
            mapOperator(StandardLibrary.booleanEquality) { left, right ->
                PsInfixOperation(left, "-ceq", right)
            }
        }
    }

    private fun FunctionMapper.Builder<PsExpression>.mapOperator(operator: OperatorDefinition, builder: (PsExpression, PsExpression) -> PsExpression) {
        map(operator) { expression, functionMapper ->
            val left = toExpression(expression.args[0], functionMapper)
            val right = toExpression(expression.args[1], functionMapper)
            builder(left, right)
        }
    }

    private fun toStringExpression(expression: Expression, functionMapper: FunctionMapper<PsExpression>): PsExpression {
        return when (expression) {
            is StringLiteral -> PsString(expression.value)
            is IntLiteral -> PsString(expression.value.toString())
            is BooleanLiteral -> PsString(expression.value.toString())
            else -> convertToString(expression.type, toExpression(expression, functionMapper))
        }
    }

    private fun convertToString(type: Type, expression: PsExpression): PsExpression {
        return when (type) {
            is Type.String -> expression
            is Type.File -> fileToString(expression)
            is Type.Int -> expression
            is Type.Boolean -> booleanToString(expression)
            is Type.List -> listToString(type, expression)
            is Type.Any -> throw IllegalArgumentException()
        }
    }

    private fun listToString(type: Type.List, expression: PsExpression): PsExpression {
        val mapped = when (type.param) {
            is Type.String, Type.Int, Type.Any -> expression
            else -> PsMethodCall(
                expression, "ForEach", listOf(
                    PsScriptBlockExpression(
                        convertToString(type.param, PsVariableReference("_"))
                    )
                )
            )
        }
        val list = PsInfixOperation(mapped, "-join", PsString(", "))
        return surroundWith("[", list, "]")
    }

    private fun fileToString(expression: PsExpression): PsExpression {
        return surroundWith("File(", expression, ")")
    }

    private fun surroundWith(prefix: String, expression: PsExpression, suffix: String): PsExpression {
        return PsInfixOperation(
            PsString(prefix),
            "+",
            PsInfixOperation(
                expression, "+", PsString(suffix)
            )
        )
    }

    private fun booleanToString(psExpression: PsExpression): PsExpression {
        return PsTernaryExpression(psExpression, PsString(BooleanLiteral.TRUE), PsString(BooleanLiteral.FALSE))
    }

    private fun toExpression(expression: Expression, functionMapper: FunctionMapper<PsExpression>): PsExpression {
        return when (expression) {
            is StringLiteral -> PsString(expression.value)
            is IntLiteral -> PsInt(expression.value)
            is BooleanLiteral -> if (expression.value) PsTrue else PsFalse
            is VariableReference -> PsVariableReference(expression.target.name)
            is FunctionCallExpression -> functionMapper.map(expression)
            is ReceiverFunctionCallExpression -> mapFunctionCall(expression, functionMapper)
        }
    }

    private fun mapFunctionCall(expression: ReceiverFunctionCallExpression, functionMapper: FunctionMapper<PsExpression>): PsExpression {
        val target = expression.target
        return when (target) {
            StringFunctions.toString -> toExpression(expression.receiver, functionMapper)
            IntFunctions.toString -> {
                val receiver = expression.receiver
                when (receiver) {
                    is IntLiteral -> PsString(receiver.value.toString())
                    else -> PsMethodCall(toExpression(receiver, functionMapper), "ToString", emptyList())
                }
            }

            BooleanFunctions.toString -> {
                val receiver = expression.receiver
                when (receiver) {
                    is BooleanLiteral -> PsString(receiver.value.toString())
                    else -> booleanToString(toExpression(receiver, functionMapper))
                }
            }

            FileFunctions.toString -> {
                fileToString(toExpression(expression.receiver, functionMapper))
            }

            FileFunctions.readText -> {
                PsDotNetFunctionInvocation(
                    "[System.IO.File]::ReadAllText", listOf(
                        PsCommandInvocation("Convert-Path", listOf(toExpression(expression.receiver, functionMapper)))
                    )
                )
            }

            ListFunctions.toString -> {
                listToString(expression.receiver.type as Type.List, toExpression(expression.receiver, functionMapper))
            }

            else -> throw IllegalArgumentException("Unknown ${target.displayName} for ${expression.receiver.type}")
        }
    }

    private fun SourceGenerator.appendDeclaration(declaration: PsVariableDeclaration) {
        append("$")
        append(declaration.name)
        append("=")
        append(declaration.value)
        appendln()
    }

    private fun SourceGenerator.append(statement: PsStatement) {
        when (statement) {
            is PsVariableDeclaration -> appendDeclaration(statement)

            is PsCommandInvocation -> {
                appendCall(statement)
                appendln()
            }

            is PsPipe -> {
                appendCall(statement.left)
                append(" | ")
                appendCall(statement.right)
                appendln()
            }

            is PsIf -> {
                append("if (")
                append(statement.condition)
                append(") { ")
                appendCall(statement.statement)
                appendln(" }")
            }
        }
    }

    private fun SourceGenerator.appendCall(statement: PsCommandInvocation) {
        append(statement.command)
        for (literal in statement.args) {
            append(" ")
            append(literal)
        }
    }

    internal fun SourceGenerator.append(expression: PsExpression) {
        when (expression) {
            is PsRaw -> append(expression.value)

            is PsString -> {
                append("\"")
                for (ch in expression.value) {
                    when (ch) {
                        '`', '$' -> {
                            append('`')
                            append(ch)
                        }

                        else -> append(ch)
                    }
                }
                append("\"")
            }

            is PsInt -> append(expression.value.toString())

            is PsTrue -> append("\$true")
            is PsFalse -> append("\$false")

            is PsIsNull -> {
                append(expression.value)
                append(" -eq \$null")
            }

            is PsVariableReference -> {
                append("$")
                append(expression.name)
            }

            is PsTernaryExpression -> {
                append("(")
                append(expression.condition)
                append(" ? ")
                append(expression.left)
                append(" : ")
                append(expression.right)
                append(")")
            }

            is PsInfixOperation -> {
                append("(")
                append(expression.left)
                append(" ")
                append(expression.operator)
                append(" ")
                append(expression.right)
                append(")")
            }

            is PsScriptBlockExpression -> {
                append("{")
                append(expression.value)
                append("}")
            }

            is PsMethodCall -> {
                append(expression.receiver)
                append(".")
                append(expression.name)
                append("(")
                append(expression.arguments, ", ") { argument ->
                    append(argument)
                }
                append(")")
            }

            is PsArrayOperator -> {
                when (expression.elements.size) {
                    0 -> append("@()")
                    1 -> {
                        append("(,")
                        append(expression.elements.first())
                        append(")")
                    }

                    else -> {
                        append("(")
                        append(expression.elements, ",") { element ->
                            append(element)
                        }
                        append(")")
                    }
                }
            }

            is PsCommandInvocation -> {
                append("(")
                appendCall(expression)
                append(")")
            }

            is PsDotNetFunctionInvocation -> {
                append("(")
                append(expression.qualifiedName)
                append("(")
                append(expression.arguments, ", ") { argument ->
                    append(argument)
                }
                append("))")
            }
        }
    }
}