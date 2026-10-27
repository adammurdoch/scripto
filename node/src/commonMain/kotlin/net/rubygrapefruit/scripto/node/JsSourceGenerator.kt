package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.append

internal fun SourceGenerator.append(declaration: JsDeclaration) {
    when (declaration) {
        is JsRequire -> {
            append("const ")
            append(declaration.name)
            append(" = require(\"")
            append(declaration.module)
            appendln("\");")
        }
    }
}

internal fun SourceGenerator.append(statement: JsStatement, mapping: VariableMapping) {
    when (statement) {
        is JsVariableDeclaration -> {
            append("let ")
            append(statement.name)
            append(" = ")
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
            for (ch in expression.value) {
                when (ch) {
                    '\\' -> {
                        append("\\")
                        append("\\")
                    }

                    else -> append(ch)
                }
            }
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
            append(' ')
            append(expression.operator)
            append(' ')
            append(expression.right, mapping)
        }

        is JsArrayLiteral -> {
            append("[")
            append(expression.elements, ", ") { element ->
                append(element, mapping)
            }
            append("]")
        }

        is JsLambda -> {
            append("(")
            append(expression.argName)
            append(") => ")
            append(expression.value, mapping)
        }

        is JsFunctionCall -> appendCall(expression, mapping)
    }
}