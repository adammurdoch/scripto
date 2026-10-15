package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.writeTo
import net.rubygrapefruit.scripto.ir.*

class PowershellGenerator {
    fun generate(program: Program, outputFile: RegularFile) {
        val mapping = OperationMapper.of {
            mapFun(StandardLibrary.printlnNoArgs) {
                PsCommandInvocation("Write-Output", listOf(PsString("")))
            }
            mapFun(StandardLibrary.printlnOneArg) {
                val args = it.arguments.map { toStringExpression(it) }
                PsCommandInvocation("Write-Output", args)
            }
            flatMapVar {
                val value = it.value
                if (value is FunctionCallExpression && value.target == StandardLibrary.readText) {
                    readFileIntoVar(value.args.first(), it.name)
                } else {
                    val initializer = toExpression(value)
                    listOf(PsVariableDeclaration(it, initializer))
                }
            }
        }

        val powershellProgram = PsProgram(mapping.map(program.operations))

        writeTo(outputFile) {
            appendln("# generated file")
            appendln()

            for (statement in powershellProgram.statements) {
                append(statement)
            }
        }
    }

    private fun toStringExpression(expression: Expression): PsExpression {
        return when (expression) {
            is StringLiteral -> PsString(expression.value)
            is IntLiteral -> PsString(expression.value.toString())
            is BooleanLiteral -> PsString(expression.value.toString())
            is VariableReference -> convertToString(expression, PsVariableReference(expression.target.name))
            is InfixFunctionCall -> convertToString(expression, mapInfix(expression))
            is FunctionCallExpression -> convertToString(expression, mapFunctionCall(expression))
            is ReceiverFunctionCallExpression -> convertToString(expression, mapFunctionCall(expression))
        }
    }

    private fun convertToString(expression: Expression, psExpression: PsExpression): PsExpression {
        return when (expression.type) {
            is Type.String -> psExpression
            is Type.Int -> psExpression
            is Type.Boolean -> PsTernaryExpression(psExpression, PsString(BooleanLiteral.TRUE), PsString(BooleanLiteral.FALSE))
        }
    }

    private fun toExpression(expression: Expression): PsExpression {
        return when (expression) {
            is StringLiteral -> PsString(expression.value)
            is IntLiteral -> PsInt(expression.value)
            is BooleanLiteral -> if (expression.value) PsTrue else PsFalse
            is VariableReference -> PsVariableReference(expression.target.name)
            is InfixFunctionCall -> mapInfix(expression)
            is FunctionCallExpression -> mapFunctionCall(expression)
            is ReceiverFunctionCallExpression -> mapFunctionCall(expression)
        }
    }

    private fun mapFunctionCall(expression: FunctionCallExpression): PsExpression {
        return if (expression.target == StandardLibrary.readText) {
            val commands = readFileIntoVar(expression.args.first(), "TMP_VAR")
            PsCommandInvocationExpression("TMP_VAR", commands)
        } else if (expression.target == StandardLibrary.fileConstructor) {
            toExpression(expression.args.first())
        } else {
            throw IllegalArgumentException("Unknown function '${expression.target.name}'")
        }
    }

    private fun mapFunctionCall(expression: ReceiverFunctionCallExpression): PsExpression {
        return if (expression.target == StandardLibrary.stringToString) {
            toExpression(expression.receiver)
        } else {
            throw IllegalArgumentException("Unknown function '${expression.target.name}'")
        }
    }

    private fun readFileIntoVar(file: Expression, varName: String): List<PsStatement> {
        val readCommand = PsPipe(
            PsCommandInvocation("Get-Content -raw", listOf(toExpression(file))),
            PsCommandInvocation("Set-Variable $varName", emptyList())
        )
        // Does not set variable when file is empty
        val checkNotNull = PsIf(PsIsNull(PsVariableReference(varName)), PsCommandInvocation("Set-Variable $varName \"\"", emptyList()))
        return listOf(readCommand, checkNotNull)
    }

    private fun mapInfix(expression: InfixFunctionCall): PsExpression {
        return when (expression.target.operator) {
            Operator.Plus -> PsInfixOperation(toExpression(expression.left), expression.operator, toExpression(expression.right))
            Operator.Equality -> PsInfixOperation(toExpression(expression.left), "-ceq", toExpression(expression.right))
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
        statement.visitStatements {
            append(it)
        }
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

            is PsCommandInvocationExpression -> {
                append("$")
                append(expression.intermediateVariable)
            }
        }
    }
}