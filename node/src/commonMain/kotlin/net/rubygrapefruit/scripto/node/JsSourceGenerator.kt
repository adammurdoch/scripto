package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.append

internal fun SourceGenerator.append(statement: JsStatement) {
    when (statement) {
        is JsFunctionCall -> {
            append(statement.name)
            append("(")
            append(statement.arguments, ", ") { argument ->
                append(argument)
            }
            append(")")
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
    }
}