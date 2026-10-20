package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.FunctionMapper
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.writeTo
import net.rubygrapefruit.scripto.ir.*

class CGenerator {
    fun generate(program: Program, outputFile: RegularFile) {
        val functionMapper = createFunctionMapper()
        val operationMapper = createOperationMapper(functionMapper)

        val statements = mutableListOf<CStatement>()
        operationMapper.mapInto(program.operations, statements)

        val variableState = VariableState()
        val declarations = mutableSetOf<ExternalDeclaration>()
        for (statement in statements) {
            statement.visitDeclarations { declaration ->
                declaration.visitHeaders {
                    declarations.add(it)
                }
                declarations.add(declaration)
            }
            statement.visitExpressions { expression ->
                if (expression is CTemporaryValue) {
                    variableState.assign(expression)
                }
            }
        }

        writeTo(outputFile) {
            appendln("// generated file")

            for (header in declarations.filterIsInstance<Header>()) {
                append("#include ")
                appendln(header.name)
            }
            for (fragment in declarations.filterIsInstance<Snippet>()) {
                appendln()
                append(fragment.text)
            }

            appendln()
            appendln("int main() {")
            for (statement in statements) {
                append(statement, variableState)
            }
            appendln("  return 0;")
            appendln('}')
        }
    }

    private fun createOperationMapper(functionMapper: FunctionMapper<CExpression>): OperationMapper<CStatement> {
        return OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                CFunctionCall("puts", listOf(CString("")), Header.Stdio)
            }
            map(StandardLibrary.printlnOneArg) { call ->
                println(call, functionMapper)
            }
            map { declaration ->
                val value = declaration.value
                val initializer = toExpression(value, functionMapper)
                val header = if (declaration.type is Type.Boolean) Header.Stdbool else null
                CVariableDeclaration(declaration, initializer, header)
            }
        }
    }

    private fun createFunctionMapper(): FunctionMapper<CExpression> {
        return FunctionMapper.of {
            map(StandardLibrary.fileConstructor) { expression, functionMapper ->
                toExpression(expression.args.first(), functionMapper)
            }
            map(StandardLibrary.listConstructor) { expression, target, functionMapper ->
                val elements = CTemporaryValue("char**", CString("??"))
                CTemporaryValue(TypeSnippet.ListStruct, elements)
            }
            mapOperator(StandardLibrary.stringConcatenation) { left, right ->
                concatString(left, right)
            }
            mapOperator(StandardLibrary.stringEquality) { left, right ->
                CInfixOperator(
                    CFunctionCall("strcmp", listOf(left, right), Header.String),
                    "==",
                    CInt(0)
                )
            }
            mapOperator(StandardLibrary.intEquality) { left, right -> CInfixOperator(left, "==", right) }
            mapOperator(StandardLibrary.intAddition) { left, right -> CInfixOperator(left, "+", right) }
            mapOperator(StandardLibrary.booleanEquality) { left, right -> CInfixOperator(left, "==", right) }
        }
    }

    private fun FunctionMapper.Builder<CExpression>.mapOperator(operator: OperatorDefinition, builder: (CExpression, CExpression) -> CExpression) {
        map(operator) { expression, functionMapper ->
            val left = toExpression(expression.args[0], functionMapper)
            val right = toExpression(expression.args[1], functionMapper)
            builder(left, right)
        }
    }

    private fun toExpression(expression: Expression, functionMapper: FunctionMapper<CExpression>): CExpression {
        return when (expression) {
            is StringLiteral -> CString(expression.value)
            is IntLiteral -> CInt(expression.value)
            is BooleanLiteral -> if (expression.value) CTrue else CFalse
            is VariableReference -> CVariableReference(expression)
            is FunctionCallExpression -> functionMapper.map(expression)
            is ReceiverFunctionCallExpression -> mapFunctionCall(expression, functionMapper)
        }
    }

    private fun mapFunctionCall(expression: ReceiverFunctionCallExpression, functionMapper: FunctionMapper<CExpression>): CExpression {
        return if (expression.target == StringFunctions.toString) {
            toExpression(expression.receiver, functionMapper)
        } else if (expression.target == FileFunctions.toString) {
            val receiver = expression.receiver
            fileToString(receiver, functionMapper)
        } else if (expression.target == IntFunctions.toString) {
            intToString(expression.receiver, functionMapper)
        } else if (expression.target == BooleanFunctions.toString) {
            booleanToString(expression.receiver, functionMapper)
        } else if (expression.target == FileFunctions.readText) {
            CFunctionCall(FunctionSnippet.ReadText, listOf(toExpression(expression.receiver, functionMapper)))
        } else {
            throw IllegalArgumentException("Unknown ${expression.target.displayName} for ${expression.receiver.type}")
        }
    }

    private fun concatString(left: CExpression, right: CExpression): CExpression {
        return CFunctionCall(FunctionSnippet.StringConcatenation, listOf(left, right))
    }

    private fun println(call: FunctionCall, functionMapper: FunctionMapper<CExpression>): CStatement {
        val arg = call.arguments.first()
        return when (arg.type) {
            is Type.String -> CFunctionCall("puts", listOf(toExpression(arg, functionMapper)), Header.Stdio)
            is Type.Boolean -> CFunctionCall("puts", listOf(booleanToString(arg, functionMapper)), Header.Stdio)
            is Type.Int -> {
                when (arg) {
                    is IntLiteral -> CFunctionCall("puts", listOf(CString(arg.value.toString())), Header.Stdio)
                    else -> CFunctionCall("printf", listOf(CString("%d\n"), toExpression(arg, functionMapper)), Header.Stdio)
                }
            }

            is Type.File -> CFunctionCall("printf", listOf(CString("File(%s)\n"), toExpression(arg, functionMapper)), Header.Stdio)
            is Type.List -> CFunctionCall("puts", listOf(CString("[??]")), Header.Stdio)

            is Type.Any -> throw IllegalArgumentException()
        }
    }

    private fun booleanToString(expression: Expression, functionMapper: FunctionMapper<CExpression>): CExpression {
        return when (expression) {
            is BooleanLiteral -> CString(expression.value.toString())
            else -> {
                val value = toExpression(expression, functionMapper)
                CTernaryExpression(value, CString(BooleanLiteral.TRUE), CString(BooleanLiteral.FALSE))
            }
        }
    }

    private fun intToString(expression: Expression, functionMapper: FunctionMapper<CExpression>): CExpression {
        return when (expression) {
            is IntLiteral -> CString(expression.value.toString())
            else -> {
                val value = toExpression(expression, functionMapper)
                CFunctionCall(FunctionSnippet.IntToString, listOf(value))
            }
        }
    }

    private fun fileToString(expression: Expression, functionMapper: FunctionMapper<CExpression>): CExpression {
        return concatString(CString("File("), concatString(toExpression(expression, functionMapper), CString(")")))
    }

    private fun SourceGenerator.append(statement: CStatement, state: VariableState) {
        when (statement) {
            is CVariableDeclaration -> append(statement, state)
            is CFunctionCall -> append(statement, state)
        }
    }

    private fun SourceGenerator.append(declaration: CVariableDeclaration, state: VariableState) {
        append("  ")
        when (declaration.type) {
            is Type.String, Type.File -> append("char *")
            is Type.Int -> append("int32_t")
            is Type.Boolean -> append("bool")
            is Type.List -> append(TypeSnippet.ListStruct.name)
            is Type.Any -> throw IllegalArgumentException()
        }
        append(" ")
        append(declaration.name)
        append(" = ")
        when (declaration.value) {
            is CTemporaryValue -> append(declaration.value.value, state)
            else -> append(declaration.value, state)
        }
        appendln(";")
    }

    private fun SourceGenerator.append(statement: CFunctionCall, state: VariableState) {
        append("  ")
        appendCall(statement, state)
        appendln(";")
    }

    private fun SourceGenerator.appendCall(statement: CFunctionCall, state: VariableState) {
        append(statement.name)
        append('(')
        for (index in statement.arguments.indices) {
            val argument = statement.arguments[index]
            if (index > 0) {
                append(", ")
            }
            append(argument, state)
        }
        append(")")
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

            is CTernaryExpression -> {
                append(expression.condition, state)
                append("?")
                append(expression.left, state)
                append(":")
                append(expression.right, state)
            }

            is CInfixOperator -> {
                append(expression.left, state)
                append(' ')
                append(expression.operator)
                append(' ')
                append(expression.right, state)
            }

            is CFunctionCall -> {
                appendCall(expression, state)
            }

            is CTemporaryValue -> {
                append(state.varFor(expression))
            }
        }
    }

    private class VariableState {
        private val expressions = mutableMapOf<CTemporaryValue, String>()

        fun assign(expression: CTemporaryValue) {
            if (!expressions.containsKey(expression)) {
                val varName = "_tmp_${expressions.size + 1}"
                expressions[expression] = varName
            }
        }

        fun varFor(expression: CTemporaryValue): String {
            return expressions.getValue(expression)
        }
    }
}