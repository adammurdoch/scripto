package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.append

internal fun SourceGenerator.append(statement: JsStatement) {
    when (statement) {
        is JsVariableDeclaration -> {
            append("const ")
            append(statement.name)
            append("=")
            append(statement.value)
            appendln(";")
        }

        is JsFunctionCall -> {
            appendCall(statement)
            appendln(";")
        }
    }
}

private fun SourceGenerator.appendCall(statement: JsFunctionCall) {
    if (statement.receiver is JsInt || statement.receiver is JsInfix) {
        append("(")
        append(statement.receiver)
        append(")")
    } else {
        append(statement.receiver)
    }
    append(".")
    append(statement.name)
    append("(")
    append(statement.arguments, ", ") { argument ->
        append(argument)
    }
    append(")")
}

private fun SourceGenerator.append(expression: JsExpression) {
    when (expression) {
        is JsString -> {
            append("\"")
            append(expression.value)
            append("\"")
        }

        is JsInt -> append(expression.value.toString())

        is JsBoolean -> {
            if (expression.value) {
                append("true")
            } else {
                append("false")
            }
        }

        is JsVariableReference -> {
            append(expression.name)
        }

        is JsInfix -> {
            append(expression.left)
            append(expression.operator)
            append(expression.right)
        }

        is JsArrayLiteral -> {
            append("[")
            append(expression.elements, ", ") { element ->
                append(element)
            }
            append("]")
        }

        is JsFunctionCall -> appendCall(expression)
    }
}