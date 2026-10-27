package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.append

internal fun SourceGenerator.append(statement: PsStatement) {
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

private fun SourceGenerator.appendDeclaration(declaration: PsVariableDeclaration) {
    append("$")
    append(declaration.name)
    append("=")
    append(declaration.value)
    appendln()
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

