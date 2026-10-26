package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.VariableState
import net.rubygrapefruit.scripto.generator.append

internal fun SourceGenerator.append(statement: BashStatement, state: VariableState) {
    when (statement) {
        is BashVariableAssignment -> append(statement, state)
        is BashVariableValueAppend -> append(statement, state)
        is BashCommandInvocation -> append(statement, state)
        is BashIfStatement -> append(statement, state)
        is BashForLoop -> append(statement, state)
    }
}

private fun SourceGenerator.append(statement: BashVariableAssignment, state: VariableState) {
    append(statement.target, state)
    append("=")
    append(statement.value, state)
    appendln()
}

private fun SourceGenerator.append(target: BashVariable, state: VariableState) {
    when (target) {
        is BashVariable.Named -> append(target.name)
        is BashVariable.Temporary -> append(state.varFor(target))
    }
}

private fun SourceGenerator.append(statement: BashVariableValueAppend, state: VariableState) {
    append(statement.name)
    append("+=")
    append(statement.value, state)
    appendln()
}

private fun SourceGenerator.append(statement: BashCommandInvocation, state: VariableState) {
    append(statement.command)
    for (arg in statement.arguments) {
        append(" ")
        append(arg, state)
    }
    if (statement.redirectInput != null) {
        append("<")
        append(statement.redirectInput, state)
    }
    appendln()
}

private fun SourceGenerator.append(statement: BashIfStatement, state: VariableState) {
    append("if [ ")
    for (arg in statement.test) {
        append(arg, state)
        append(' ')
    }
    appendln("]; then")
    append(statement.trueBranch, state)
    appendln("else")
    append(statement.falseBranch, state)
    appendln("fi")
}

private fun SourceGenerator.append(statement: BashForLoop, state: VariableState) {
    append("for ")
    append(statement.elementVarName)
    append(" in ")
    append(statement.elements, state)
    append("; do\n")
    for (loopStatement in statement.statements) {
        append(loopStatement, state)
    }
    append("done\n")
}

private fun SourceGenerator.append(expression: BashExpression, state: VariableState) {
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

        is BashVariableReference -> {
            if (expression.quoted) {
                append("\"$")
                append(expression.variable, state)
                append("\"")
            } else {
                append("$")
                append(expression.variable, state)
            }
        }

        is BashVariableName -> {
            append(expression.variable, state)
        }

        is BashConcatenatedExpressions -> {
            for (nested in expression.values) {
                append(nested, state)
            }
        }

        is BashArithmetic -> {
            append("$((")
            append(expression.left, state)
            append(expression.operator)
            append(expression.right, state)
            append("))")
        }

        is BashArrayLiteral -> {
            append("(")
            append(expression.elements, " ") { element ->
                append(element, state)
            }
            append(")")
        }

        is BashCommandInvocationExpression -> {
            append("\"$")
            append(expression.result, state)
            append("\"")
        }
    }
}
