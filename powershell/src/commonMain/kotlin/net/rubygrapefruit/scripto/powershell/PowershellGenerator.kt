package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.StatementScope
import net.rubygrapefruit.scripto.generator.writeTo
import net.rubygrapefruit.scripto.ir.*

class PowershellGenerator {
    fun generate(program: Program, outputFile: RegularFile) {
        val scope = StatementScope()
        val operationMapper = createOperationMapper(scope)

        val statements = operationMapper.map(program.operations)
        val powershellProgram = PsProgram(statements)

        writeTo(outputFile) {
            appendln("# generated file")
            appendln()

            for (statement in powershellProgram.statements) {
                append(statement)
            }
        }
    }

    private fun createOperationMapper(scope: StatementScope): OperationMapper<PsStatement> {
        return OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                PsCommandInvocation("Write-Output", listOf(PsString("")))
            }
            map(StandardLibrary.printlnOneArg) { call ->
                val args = call.arguments.map { toStringExpression(it, scope) }
                PsCommandInvocation("Write-Output", args)
            }
            map { declaration ->
                val value = declaration.value
                val initializer = toExpression(value, scope)
                PsVariableDeclaration(declaration, initializer)
            }
            collectDependencies { operation: PsStatement ->
                addDependencies(operation)
            }
        }
    }

    private fun toStringExpression(expression: Expression, scope: StatementScope): PsExpression {
        return when (expression) {
            is StringLiteral -> PsString(expression.value)
            is IntLiteral -> PsString(expression.value.toString())
            is BooleanLiteral -> PsString(expression.value.toString())
            is VariableReference -> convertToString(expression, PsVariableReference(expression.target.name))
            is FunctionCallExpression -> convertToString(expression, mapFunctionCall(expression, scope))
            is ReceiverFunctionCallExpression -> convertToString(expression, mapFunctionCall(expression, scope))
        }
    }

    private fun convertToString(expression: Expression, psExpression: PsExpression): PsExpression {
        return when (expression.type) {
            is Type.String -> psExpression
            is Type.File -> fileToString(psExpression)
            is Type.Int -> psExpression
            is Type.Boolean -> convertBooleanToString(psExpression)
        }
    }

    private fun fileToString(psExpression: PsExpression): PsExpression {
        return PsInfixOperation(
            PsString("File("), "+",
            PsInfixOperation(psExpression, "+", PsString(")"))
        )
    }

    private fun convertBooleanToString(psExpression: PsExpression): PsExpression {
        return PsTernaryExpression(psExpression, PsString(BooleanLiteral.TRUE), PsString(BooleanLiteral.FALSE))
    }

    private fun toExpression(expression: Expression, scope: StatementScope): PsExpression {
        return when (expression) {
            is StringLiteral -> PsString(expression.value)
            is IntLiteral -> PsInt(expression.value)
            is BooleanLiteral -> if (expression.value) PsTrue else PsFalse
            is VariableReference -> PsVariableReference(expression.target.name)
            is FunctionCallExpression -> mapFunctionCall(expression, scope)
            is ReceiverFunctionCallExpression -> mapFunctionCall(expression, scope)
        }
    }

    private fun mapFunctionCall(expression: FunctionCallExpression, scope: StatementScope): PsExpression {
        val target = expression.target
        return when (target) {
            StandardLibrary.fileConstructor -> toExpression(expression.args.first(), scope)
            is OperatorDefinition -> {
                when (target.operator) {
                    Operator.Plus -> PsInfixOperation(toExpression(expression.args[0], scope), target.operator.operator, toExpression(expression.args[1], scope))
                    Operator.Equality -> PsInfixOperation(toExpression(expression.args[0], scope), "-ceq", toExpression(expression.args[1], scope))
                }
            }

            else -> throw IllegalArgumentException("Unknown ${target.displayName}")
        }
    }

    private fun mapFunctionCall(expression: ReceiverFunctionCallExpression, scope: StatementScope): PsExpression {
        return when (expression.target) {
            StringFunctions.toString -> toExpression(expression.receiver, scope)
            IntFunctions.toString -> {
                val receiver = expression.receiver
                when (receiver) {
                    is IntLiteral -> PsString(receiver.value.toString())
                    else -> PsFunctionCall(toExpression(receiver, scope), "ToString")
                }
            }

            BooleanFunctions.toString -> {
                val receiver = expression.receiver
                when (receiver) {
                    is BooleanLiteral -> PsString(receiver.value.toString())
                    else -> convertBooleanToString(toExpression(receiver, scope))
                }
            }

            FileFunctions.toString -> {
                fileToString(toExpression(expression.receiver, scope))
            }

            FileFunctions.readText -> {
                val varName = scope.newVarName("read_text")
                val commands = readFileIntoVar(toExpression(expression.receiver, scope), varName)
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

            is PsCommandInvocationExpression -> {
                append("$")
                append(expression.intermediateVariable)
            }
        }
    }
}