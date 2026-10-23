package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.*
import net.rubygrapefruit.scripto.ir.*

class PowershellGenerator {
    fun generate(program: Program, outputFile: RegularFile) {
        val scope = StatementScope()
        val functionMapper = createFunctionMapper(scope)
        val operationMapper = createOperationMapper(functionMapper, scope)

        val statements = operationMapper.map(program.operations)
        val powershellProgram = PsProgram(statements)

        writeTo(outputFile, "#") {
            for (statement in powershellProgram.statements) {
                append(statement)
            }
        }
    }

    private fun createOperationMapper(functionMapper: FunctionMapper<PsExpression>, scope: StatementScope): OperationMapper<PsStatement> {
        return OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                PsCommandInvocation("Write-Output", listOf(PsString("")))
            }
            map(StandardLibrary.printlnOneArg) { call ->
                val args = call.arguments.map { toStringExpression(it, scope, functionMapper) }
                PsCommandInvocation("Write-Output", args)
            }
            map { declaration ->
                val value = declaration.value
                val initializer = toExpression(value, scope, functionMapper)
                PsVariableDeclaration(declaration, initializer)
            }
            collectDependencies { operation: PsStatement ->
                addDependencies(operation)
            }
        }
    }

    private fun createFunctionMapper(scope: StatementScope): FunctionMapper<PsExpression> {
        return FunctionMapper.of {
            map(StandardLibrary.fileConstructor) { expression, functionMapper ->
                toExpression(expression.args.first(), scope, functionMapper)
            }
            map(StandardLibrary.listConstructor) { expression, _, functionMapper ->
                PsArrayOperator(expression.args.map { toExpression(it, scope, functionMapper) })
            }
            mapOperator(StandardLibrary.stringEquality, scope) { left, right ->
                PsInfixOperation(left, "-ceq", right)
            }
            mapOperator(StandardLibrary.stringConcatenation, scope) { left, right ->
                PsInfixOperation(left, "+", right)
            }
            mapOperator(StandardLibrary.intEquality, scope) { left, right ->
                PsInfixOperation(left, "-ceq", right)
            }
            mapOperator(StandardLibrary.intAddition, scope) { left, right ->
                PsInfixOperation(left, "+", right)
            }
            mapOperator(StandardLibrary.booleanEquality, scope) { left, right ->
                PsInfixOperation(left, "-ceq", right)
            }
        }
    }

    private fun FunctionMapper.Builder<PsExpression>.mapOperator(operator: OperatorDefinition, scope: StatementScope, builder: (PsExpression, PsExpression) -> PsExpression) {
        map(operator) { expression, functionMapper ->
            val left = toExpression(expression.args[0], scope, functionMapper)
            val right = toExpression(expression.args[1], scope, functionMapper)
            builder(left, right)
        }
    }

    private fun toStringExpression(expression: Expression, scope: StatementScope, functionMapper: FunctionMapper<PsExpression>): PsExpression {
        return when (expression) {
            is StringLiteral -> PsString(expression.value)
            is IntLiteral -> PsString(expression.value.toString())
            is BooleanLiteral -> PsString(expression.value.toString())
            is VariableReference -> convertToString(expression, PsVariableReference(expression.target.name))
            is FunctionCallExpression -> convertToString(expression, functionMapper.map(expression))
            is ReceiverFunctionCallExpression -> convertToString(expression, mapFunctionCall(expression, scope, functionMapper))
        }
    }

    private fun convertToString(expression: Expression, psExpression: PsExpression): PsExpression {
        return when (expression.type) {
            is Type.String -> psExpression
            is Type.File -> fileToString(psExpression)
            is Type.Int -> psExpression
            is Type.Boolean -> booleanToString(psExpression)
            is Type.List -> listToString(psExpression)
            is Type.Any -> throw IllegalArgumentException()
        }
    }

    private fun listToString(expression: PsExpression): PsExpression {
        val list = PsInfixOperation(
            expression,
            "-join",
            PsString(", ")
        )
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

    private fun toExpression(expression: Expression, scope: StatementScope, functionMapper: FunctionMapper<PsExpression>): PsExpression {
        return when (expression) {
            is StringLiteral -> PsString(expression.value)
            is IntLiteral -> PsInt(expression.value)
            is BooleanLiteral -> if (expression.value) PsTrue else PsFalse
            is VariableReference -> PsVariableReference(expression.target.name)
            is FunctionCallExpression -> functionMapper.map(expression)
            is ReceiverFunctionCallExpression -> mapFunctionCall(expression, scope, functionMapper)
        }
    }

    private fun mapFunctionCall(expression: ReceiverFunctionCallExpression, scope: StatementScope, functionMapper: FunctionMapper<PsExpression>): PsExpression {
        return when (expression.target) {
            StringFunctions.toString -> toExpression(expression.receiver, scope, functionMapper)
            IntFunctions.toString -> {
                val receiver = expression.receiver
                when (receiver) {
                    is IntLiteral -> PsString(receiver.value.toString())
                    else -> PsFunctionCall(toExpression(receiver, scope, functionMapper), "ToString")
                }
            }

            BooleanFunctions.toString -> {
                val receiver = expression.receiver
                when (receiver) {
                    is BooleanLiteral -> PsString(receiver.value.toString())
                    else -> booleanToString(toExpression(receiver, scope, functionMapper))
                }
            }

            FileFunctions.toString -> {
                fileToString(toExpression(expression.receiver, scope, functionMapper))
            }

            FileFunctions.readText -> {
                val varName = scope.newVarName("read_text")
                val commands = readFileIntoVar(toExpression(expression.receiver, scope, functionMapper), varName)
                PsCommandInvocationExpression(varName, commands)
            }

            else -> throw IllegalArgumentException("Unknown ${expression.target.displayName} for ${expression.receiver.type}")
        }
    }

    private fun readFileIntoVar(file: PsExpression, varName: String): List<PsStatement> {
        val readCommand = PsPipe(
            PsCommandInvocation("Get-Content -raw", listOf(file)),
            PsCommandInvocation("Set-Variable $varName", emptyList())
        )
        // Does not set variable when file is empty
        val checkNotNull = PsIf(PsIsNull(PsVariableReference(varName)), PsCommandInvocation("Set-Variable $varName \"\"", emptyList()))
        return listOf(readCommand, checkNotNull)
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
                appendNoNewLine(statement)
                appendln()
            }

            is PsPipe -> {
                appendNoNewLine(statement.left)
                append(" | ")
                appendNoNewLine(statement.right)
                appendln()
            }

            is PsIf -> {
                append("if (")
                append(statement.condition)
                append(") { ")
                appendNoNewLine(statement.statement)
                appendln(" }")
            }
        }
    }

    private fun SourceGenerator.appendNoNewLine(statement: PsCommandInvocation) {
        append(statement.command)
        for (literal in statement.args) {
            append(" ")
            append(literal)
        }
    }

    internal fun SourceGenerator.append(expression: PsExpression) {
        when (expression) {
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

            is PsFunctionCall -> {
                append(expression.receiver)
                append(".")
                append(expression.name)
                append("()")
            }

            is PsArrayOperator -> {
                when (expression.elements.size) {
                    0 -> append("@()")
                    1 -> {
                        append(",")
                        append(expression.elements.first())
                    }

                    else -> {
                        append(expression.elements, ",") { element ->
                            append(element)
                        }
                    }
                }
            }

            is PsCommandInvocationExpression -> {
                append("$")
                append(expression.intermediateVariable)
            }
        }
    }
}