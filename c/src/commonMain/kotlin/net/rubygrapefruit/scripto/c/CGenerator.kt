package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.*
import net.rubygrapefruit.scripto.ir.*

class CGenerator {
    fun generate(program: Program, outputFile: RegularFile) {
        val functionMapper = createFunctionMapper()
        val operationMapper = createOperationMapper(functionMapper)

        val statements = mutableListOf<CStatement>()
        operationMapper.mapInto(program.operations, statements)

        val variableState = VariableState()
        val declarations = mutableSetOf<ExternalDeclaration>()
        var index = 0
        while (index < statements.size) {
            val original = statements[index]
            val statement = if (original is CVariableDeclaration && original.value is CTemporaryValue) {
                val replacement = CVariableDeclaration(original.name, original.type, original.value.value)
                statements[index] = replacement
                replacement
            } else {
                original
            }
            statement.visitDeclarations { declaration ->
                declaration.visitDeclarations {
                    declarations.add(it)
                }
                declarations.add(declaration)
            }
            statement.visitExpressions { expression ->
                if (expression is CTemporaryValue) {
                    val varName = variableState.assign(expression)
                    statements.add(index, CVariableDeclaration(varName, expression.type, expression.value))
                    index++
                }
            }
            index++
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
            appendln("    return 0;")
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
                CVariableDeclaration(declaration.name, toCType(declaration.type), initializer)
            }
        }
    }

    private fun toCType(type: Type): CType {
        return when (type) {
            is Type.String, Type.File -> CType.CString
            is Type.Int -> CType.Int32
            is Type.Boolean -> CType.Bool
            is Type.List -> TypeSnippet.ListStruct.type
            is Type.Any -> throw IllegalArgumentException()
        }
    }

    private fun createFunctionMapper(): FunctionMapper<CExpression> {
        return FunctionMapper.of {
            map(StandardLibrary.fileConstructor) { expression, functionMapper ->
                toExpression(expression.args.first(), functionMapper)
            }
            map(StandardLibrary.listConstructor) { expression, definition, functionMapper ->
                if (expression.args.isEmpty()) {
                    CTemporaryValue(TypeSnippet.ListStruct, CInitializer(listOf(CInt(0), CNull)))
                } else {
                    val elements = expression.args.map { toExpression(it, functionMapper) }
                    val elementArrayType = toCType(definition.argument).array()
                    CTemporaryValue(
                        TypeSnippet.ListStruct,
                        CInitializer(
                            listOf(
                                CInt(elements.size),
                                CTemporaryValue(
                                    elementArrayType,
                                    CInitializer(elements)
                                )
                            )
                        )
                    )
                }
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
        } else if (expression.target == ListFunctions.toString) {
            listToString(expression.receiver, functionMapper)
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
            is Type.List -> CFunctionCall("puts", listOf(listToString(arg, functionMapper)), Header.Stdio)

            is Type.Any -> throw IllegalArgumentException()
        }
    }

    private fun listToString(expression: Expression, functionMapper: FunctionMapper<CExpression>): CExpression {
        if ((expression.type as Type.List).param != Type.String) {
            return CString("??")
        }
        val value = toExpression(expression, functionMapper)
        return CFunctionCall(FunctionSnippet.listToString, listOf(CPointer(value)))
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
        append("    ")
        when (statement) {
            is CVariableDeclaration -> append(statement, state)
            is CFunctionCall -> append(statement, state)
        }
    }

    private fun SourceGenerator.append(declaration: CVariableDeclaration, state: VariableState) {
        append(declaration.type.declaration)
        append(" ")
        append(declaration.name)
        if (declaration.type is CType.Array) {
            repeat(declaration.type.dimensions) {
                append("[]")
            }
        }
        append(" = ")
        append(declaration.value, state)
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

    private class VariableState {
        private val expressions = mutableMapOf<CTemporaryValue, String>()

        fun assign(expression: CTemporaryValue): String {
            val varName = "_tmp_${expressions.size + 1}"
            expressions[expression] = varName
            return varName
        }

        fun varFor(expression: CTemporaryValue): String {
            return expressions.getValue(expression)
        }
    }
}