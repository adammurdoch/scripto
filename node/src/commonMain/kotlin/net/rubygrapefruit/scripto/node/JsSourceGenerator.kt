package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.append

internal fun SourceGenerator.append(statement: JsStatement, mapping: VariableMapping) {
    when (statement) {
        is JsVariableDeclaration -> {
            append("const ")
            append(statement.name)
            append("=")
            append(statement.value, mapping)
            appendln(";")
        }

        is JsFunctionCall -> {
            appendCall(statement, mapping)
            appendln(";")
        }
    }
}

private fun SourceGenerator.appendCall(statement: JsFunctionCall, mapping: VariableMapping) {
    if (statement.receiver is JsInt || statement.receiver is JsInfix) {
        append("(")
        append(statement.receiver, mapping)
        append(")")
    } else {
        append(statement.receiver, mapping)
    }
    append(".")
    append(statement.name)
    append("(")
    append(statement.arguments, ", ") { argument ->
        append(argument, mapping)
    }
    append(")")
}

private fun SourceGenerator.append(expression: JsExpression, mapping: VariableMapping) {
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
            val mapped = mapping.map(expression)
            if (mapped != null) {
                append(mapped, mapping)
            } else {
                append(expression.name)
            }
        }

        is JsInfix -> {
            append(expression.left, mapping)
            append(expression.operator)
            append(expression.right, mapping)
        }

        is JsArrayLiteral -> {
            append("[")
            append(expression.elements, ", ") { element ->
                append(element, mapping)
            }
            append("]")
        }

        is JsFunctionCall -> appendCall(expression, mapping)
    }
}