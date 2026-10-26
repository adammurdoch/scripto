package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.*
import net.rubygrapefruit.scripto.ir.*

@OptIn(ExperimentalStdlibApi::class)
class CGenerator {
    private val listElementToString = mutableMapOf<Type, CGeneratedFunction>()

    fun generate(program: Program, outputFile: RegularFile) {
        val functionMapper = createFunctionMapper()
        val operationMapper = createOperationMapper(functionMapper)

        val statements = mutableListOf<CStatement>()
        operationMapper.mapInto(program.operations, statements)
        statements.add(CReturnStatement(CInt(0)))

        val variableState = VariableState()
        val declarationQueue = Declaration.Queue<CDeclaration>()
        val declarationVisitor: (CDeclaration) -> Unit = { element ->
            declarationQueue.add(element)
        }

        var index = 0
        while (index < statements.size) {
            val original = statements[index]
            val statement = if (original is CVariableDeclaration && original.value is CTemporaryValue) {
                // A variable whose value must be stored in a variable, so can store the value directly in the variable
                val replacement = CVariableDeclaration(original.name, original.type, original.value.value)
                statements[index] = replacement
                replacement
            } else {
                original
            }

            statement.visitOwnDeclarations(declarationVisitor)
            statement.visitExpressions { expression ->
                expression.visitOwnDeclarations(declarationVisitor)
                if (expression is CTemporaryValue) {
                    // Value must be stored in a variable
                    val varName = variableState.assign(expression)
                    statements.add(index, CVariableDeclaration(varName, expression.type, expression.value))
                    index++
                } else if (expression is CVariableReference && expression.name == StandardLibrary.argsVariable.name) {
                    statements.add(
                        0, CVariableDeclaration(
                            StandardLibrary.argsVariable.name, TypeSnippet.ListStruct, CInitializer(
                                listOf(
                                    CInfixOperator.minus(CVariableReference("argc"), 1),
                                    CInfixOperator.plus(CVariableReference("argv"), 1)
                                )
                            )
                        )
                    )
                    index++
                }
            }
            index++
        }

        val declarations = declarationQueue.getAll()

        writeTo(outputFile, "//") {
            for (header in declarations.filterIsInstance<Header>()) {
                append("#include ")
                appendln(header.name)
            }
            for (declaration in declarations.filterIsInstance<Snippet>()) {
                appendln()
                append(declaration.text)
            }
            for (declaration in declarations.filterIsInstance<CGeneratedFunction>()) {
                append(declaration, variableState)
            }

            val mainFunction = CGeneratedFunction("main", CType.CInt, listOf(Parameter("argc", CType.CInt), Parameter("argv", CType.CString.array())), statements)
            append(mainFunction, variableState)
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
            is Type.List -> TypeSnippet.ListStruct
            is Type.Any -> TypeSnippet.AnyStruct
        }
    }

    private fun createFunctionMapper(): FunctionMapper<CExpression> {
        return FunctionMapper.of {
            map(StandardLibrary.fileConstructor) { expression, functionMapper ->
                toExpression(expression.args.first(), functionMapper)
            }
            map(StandardLibrary.listConstructor) { expression, definition, functionMapper ->
                if (expression.args.isEmpty()) {
                    CTemporaryValue(TypeSnippet.ListStruct, "list", CInitializer(listOf(CInt(0), CNull)))
                } else {
                    val elements = expression.args.map { toExpression(it, functionMapper) }
                    val elementArrayType = toCType(definition.argument).array()
                    CTemporaryValue(
                        TypeSnippet.ListStruct,
                        "list",
                        CInitializer(
                            listOf(
                                CInt(elements.size),
                                CTemporaryValue(
                                    elementArrayType,
                                    "elements",
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

            mapReceiver(StringFunctions.toString) { expression, functionMapper ->
                toExpression(expression.receiver, functionMapper)
            }
            mapReceiver(FileFunctions.toString) { expression, functionMapper ->
                val receiver = expression.receiver
                fileToString(toExpression(receiver, functionMapper))
            }
            mapReceiver(IntFunctions.toString) { expression, functionMapper ->
                intToString(expression.receiver, functionMapper)
            }
            mapReceiver(BooleanFunctions.toString) { expression, functionMapper ->
                booleanToString(expression.receiver, functionMapper)
            }
            mapReceiver(ListFunctions.toString) { expression, functionMapper ->
                listToString(expression.receiver, functionMapper)
            }
            mapReceiver(FileFunctions.readText) { expression, functionMapper ->
                CFunctionCall(FunctionSnippet.ReadText, listOf(toExpression(expression.receiver, functionMapper)))
            }
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
            is ReceiverFunctionCallExpression -> functionMapper.map(expression)
        }
    }

    private fun concatString(left: CExpression, right: CExpression): CExpression {
        return CFunctionCall(FunctionSnippet.StringConcatenation, listOf(left, right))
    }

    private fun println(call: UnitFunctionCall, functionMapper: FunctionMapper<CExpression>): CStatement {
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

    private fun listToString(receiver: Expression, functionMapper: FunctionMapper<CExpression>): CExpression {
        val elementType = (receiver.type as Type.List).param
        val list = toExpression(receiver, functionMapper)
        return listToString(list, elementType)
    }

    private fun listToString(list: CExpression, elementType: Type): CExpression {
        return when (elementType) {
            is Type.String -> CFunctionCall(FunctionSnippet.StringListToString, listOf(CPointer(list)))
            is Type.Int -> listToString(list, elementType, CType.Int32, true) { expression -> intToString(expression) }
            is Type.Boolean -> listToString(list, elementType, CType.Bool, false) { expression -> booleanToString(expression) }
            is Type.File -> listToString(list, elementType, CType.CString, false) { expression -> fileToString(expression) }
            is Type.Any -> listToString(list, elementType, CType.Bool, false) { _ -> CString("??") }
            is Type.List -> listToString(list, elementType, TypeSnippet.ListStruct, true) { expression ->
                listToString(expression, elementType.param)
            }
        }
    }

    private fun listToString(receiver: CExpression, elementType: Type, elementCType: CType, freeMappedStrings: Boolean, toString: (CExpression) -> CExpression): CExpression {
        val elementToString = listElementToString.getOrPutIfNull(elementType) {
            FunctionSnippet.listElementToStringFunction("_${identifierForType(elementType)}_element_to_string", elementCType, toString)
        }
        return CFunctionCall(FunctionSnippet.ListToString, listOf(CPointer(receiver), CBooleanLiteral.of(freeMappedStrings), CFunctionPointer(elementToString)))
    }

    private fun identifierForType(type: Type): String {
        return when (type) {
            is Type.String -> "string"
            is Type.File -> "file"
            is Type.Int -> "int"
            is Type.Boolean -> "boolean"
            is Type.List -> "list_${identifierForType(type.param)}"
            is Type.Any -> "any"
        }
    }

    private fun booleanToString(expression: Expression, functionMapper: FunctionMapper<CExpression>): CExpression {
        return when (expression) {
            is BooleanLiteral -> CString(expression.value.toString())
            else -> {
                val value = toExpression(expression, functionMapper)
                booleanToString(value)
            }
        }
    }

    private fun booleanToString(value: CExpression): CExpression {
        return CTernaryExpression(value, CString(BooleanLiteral.TRUE), CString(BooleanLiteral.FALSE))
    }

    private fun intToString(expression: Expression, functionMapper: FunctionMapper<CExpression>): CExpression {
        return when (expression) {
            is IntLiteral -> CString(expression.value.toString())
            else -> intToString(toExpression(expression, functionMapper))
        }
    }

    private fun intToString(value: CExpression): CExpression {
        return CFunctionCall(FunctionSnippet.IntToString, listOf(value))
    }

    private fun fileToString(expression: CExpression): CExpression {
        return concatString(CString("File("), concatString(expression, CString(")")))
    }
}
