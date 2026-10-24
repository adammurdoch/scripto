package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.VariableState
import net.rubygrapefruit.scripto.generator.append

internal fun SourceGenerator.append(function: CGeneratedFunction, state: VariableState) {
    appendln()
    if (function.returnType != null) {
        append(function.returnType.declaration)
        append(" ")
    }
    append(function.name)
    append("(")
    append(function.parameters, ", ") { parameter ->
        append(parameter.type.variableDeclaration(parameter.name))
    }
    appendln(") {")
    for (statement in function.statements) {
        append(statement, state)
    }
    appendln('}')
}

private fun SourceGenerator.append(statement: CStatement, state: VariableState) {
    append("    ")
    when (statement) {
        is CVariableDeclaration -> append(statement, state)
        is CFunctionCall -> append(statement, state)
        is CReturnStatement -> append(statement, state)
    }
}

private fun SourceGenerator.append(statement: CVariableDeclaration, state: VariableState) {
    append(statement.type.variableDeclaration(statement.name))
    append(" = ")
    append(statement.value, state)
    appendln(";")
}

private fun SourceGenerator.append(statement: CFunctionCall, state: VariableState) {
    appendCall(statement, state)
    appendln(";")
}

private fun SourceGenerator.appendCall(statement: CFunctionCall, state: VariableState) {
    append(statement.name)
    append('(')
    append(statement.arguments, ",") { argument ->
        append(argument, state)
    }
    append(")")
}

private fun SourceGenerator.append(statement: CReturnStatement, state: VariableState) {
    append("return ")
    append(statement.value, state)
    appendln(";")
}

private fun SourceGenerator.append(expression: CExpression, state: VariableState) {
    when (expression) {
        is CNull -> append("NULL")

        is CString -> {
            append("\"")
            for (ch in expression.value) {
                when (ch) {
                    '\n' -> append("\\n")
                    '\\' -> append("\\\\")
                    else -> append(ch)
                }
            }
            append("\"")
        }

        is CInt -> append(expression.value.toString())
        is CTrue -> append("true")
        is CFalse -> append("false")

        is CVariableReference -> {
            append(expression.name)
        }

        is CPointer -> {
            append("&")
            append(expression.value, state)
        }

        is CFunctionPointer -> {
            append(expression.function.name)
        }

        is CStructField -> {
            append(expression.struct, state)
            append("->")
            append(expression.fieldName)
        }

        is CArrayIndex -> {
            append(expression.array, state)
            append("[")
            append(expression.index, state)
            append("]")
        }

        is CCast -> {
            append("((")
            append(expression.type.declaration)
            append(")")
            append(expression.value, state)
            append(")")
        }

        is CTernaryExpression -> {
            append(expression.condition, state)
            append(" ? ")
            append(expression.left, state)
            append(" : ")
            append(expression.right, state)
        }

        is CInfixOperator -> {
            append(expression.left, state)
            append(' ')
            append(expression.operator)
            append(' ')
            append(expression.right, state)
        }

        is CInitializer -> {
            append("{")
            append(expression.elements, ", ") { element ->
                append(element, state)
            }
            append("}")
        }

        is CFunctionCall -> {
            appendCall(expression, state)
        }

        is CTemporaryValue -> {
            append(state.varFor(expression))
        }
    }
}