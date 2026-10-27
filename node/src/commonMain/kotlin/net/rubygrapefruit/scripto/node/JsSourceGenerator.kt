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
            append(statement.name)
            append("(")
            append(statement.arguments, ", ") { argument ->
                append(argument)
            }
            appendln(");")
        }
    }
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
    }
}