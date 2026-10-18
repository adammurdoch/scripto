package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.FunctionMapper
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.writeTo
import net.rubygrapefruit.scripto.ir.*

class CGenerator {
    fun generate(program: Program, outputFile: RegularFile) {
        val scope = CStatementScope()

        val functionMapper = createFunctionMapper(scope)
        val operationMapper = createOperationMapper(scope, functionMapper)

        val statements = operationMapper.map(program.operations)
        val cProgram = CProgram(statements)

        writeTo(outputFile) {
            appendln("// generated file")

            val declarations = mutableSetOf<ExternalDeclaration>()
            for (statement in cProgram.statements) {
                statement.visitDeclarations { declaration ->
                    declaration.visitHeaders {
                        declarations.add(it)
                    }
                    declarations.add(declaration)
                }
            }
            for (header in declarations.filterIsInstance<Header>()) {
                append("#include ")
                appendln(header.name)
            }
            for (fragment in declarations.filterIsInstance<SourceFragment>()) {
                appendln()
                append(fragment.text)
            }

            appendln()
            appendln("int main() {")
            for (statement in cProgram.statements) {
                when (statement) {
                    is CVariableDeclaration -> append(statement)
                    is CFunctionCall -> append(statement)
                }
            }
            appendln("  return 0;")
            appendln('}')
        }
    }

    private fun createOperationMapper(scope: CStatementScope, functionMapper: FunctionMapper<CExpression>): OperationMapper<CStatement> {
        return OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                CFunctionCall("puts", listOf(CString("")), Header.Stdio)
            }
            map(StandardLibrary.printlnOneArg) { operation ->
                val arg = operation.arguments.first()
                when (arg.type) {
                    is Type.String -> {
                        CFunctionCall("puts", listOf(toExpression(arg, scope, functionMapper)), Header.Stdio)
                    }

                    else -> {
                        val args = toPrintfArgs(arg, scope, functionMapper)
                        CFunctionCall("printf", args, Header.Stdio)
                    }
                }
            }
            map { operation ->
                val value = operation.value
                val initializer = toExpression(value, scope, functionMapper)
                val header = if (operation.type is Type.Boolean) Header.Stdbool else null
                CVariableDeclaration(operation, initializer, header)
            }
            collectDependencies { operation ->
                when (operation) {
                    is CFunctionCall -> addDependencies(operation.arguments)
                    is CVariableDeclaration -> addDependencies(operation.value)
                }
            }
        }
    }

    private fun toExpression(expression: Expression, scope: CStatementScope, functionMapper: FunctionMapper<CExpression>): CExpression {
        return when (expression) {
            is StringLiteral -> CString(expression.value)
            is IntLiteral -> CInt(expression.value)
            is BooleanLiteral -> if (expression.value) CTrue else CFalse
            is VariableReference -> CVariableReference(expression)
            is InfixFunctionCall -> mapInfix(expression, scope, functionMapper)
            is FunctionCallExpression -> functionMapper.map(expression)
            is ReceiverFunctionCallExpression -> mapFunctionCall(expression, scope, functionMapper)
        }
    }

    private fun createFunctionMapper(scope: CStatementScope): FunctionMapper<CExpression> {
        return FunctionMapper.of {
            map(StandardLibrary.fileConstructor) { expression, functionMapper ->
                toExpression(expression.args.first(), scope, functionMapper)
            }
        }
    }

    private fun mapFunctionCall(expression: ReceiverFunctionCallExpression, scope: CStatementScope, functionMapper: FunctionMapper<CExpression>): CExpression {
        return if (expression.target == StringFunctions.toString) {
            toExpression(expression.receiver, scope, functionMapper)
        } else if (expression.target == FileFunctions.toString) {
            concatString(CString("File("), concatString(toExpression(expression.receiver, scope, functionMapper), CString(")")))
        } else if (expression.target == IntFunctions.toString) {
            val receiver = expression.receiver
            when (receiver) {
                is IntLiteral -> CString(receiver.value.toString())
                else -> {
                    val value = toExpression(expression.receiver, scope, functionMapper)
                    val valueVar = scope.newVarName("iVal")
                    val lengthVar = scope.newVarName("strLen")
                    val stringVar = scope.newVarName("str")
                    CPreCalculatedExpression(
                        listOf(
                            CVariableDeclaration(valueVar.name, Type.Int, value),
                            CVariableDeclaration(
                                lengthVar.name, Type.Int,
                                CInfixOperator(
                                    CFunctionCall("snprintf", listOf(CNull, CInt(0), CString("%d"), valueVar)),
                                    "+",
                                    CInt(1)
                                )
                            ),
                            CVariableDeclaration(
                                stringVar.name, Type.String,
                                CFunctionCall("malloc", listOf(lengthVar), Header.Stdlib)
                            ),
                            CFunctionCall("snprintf", listOf(stringVar, lengthVar, CString("%d"), valueVar), null)
                        ),
                        stringVar
                    )
                }
            }
        } else if (expression.target == BooleanFunctions.toString) {
            val receiver = expression.receiver
            when (receiver) {
                is BooleanLiteral -> CString(receiver.value.toString())
                else -> {
                    val value = toExpression(expression.receiver, scope, functionMapper)
                    CTernaryExpression(value, CString(BooleanLiteral.TRUE), CString(BooleanLiteral.FALSE))
                }
            }
        } else if (expression.target == FileFunctions.readText) {
            CFunctionCall("readText", listOf(toExpression(expression.receiver, scope, functionMapper)), SourceFragment.ReadText)
        } else {
            throw IllegalArgumentException("Unknown function '${expression.receiver.type}.${expression.target.name}()'")
        }
    }

    private fun mapInfix(expression: InfixFunctionCall, scope: CStatementScope, functionMapper: FunctionMapper<CExpression>): CExpression {
        return when (expression.target) {
            StandardLibrary.stringConcatenation -> {
                val left = toExpression(expression.left, scope, functionMapper)
                val right = toExpression(expression.right, scope, functionMapper)
                concatString(left, right)
            }

            StandardLibrary.stringEquality -> {
                val left = toExpression(expression.left, scope, functionMapper)
                val right = toExpression(expression.right, scope, functionMapper)
                CInfixOperator(
                    CFunctionCall("strcmp", listOf(left, right), Header.String),
                    "==",
                    CInt(0)
                )
            }

            else -> {
                val left = toExpression(expression.left, scope, functionMapper)
                val right = toExpression(expression.right, scope, functionMapper)
                CInfixOperator(left, expression.operator, right)
            }
        }
    }

    private fun concatString(left: CExpression, right: CExpression): CExpression {
        return CFunctionCall("stringConcat", listOf(left, right), SourceFragment.StringConcatenation)
    }

    private fun toPrintfArgs(arg: Expression, scope: CStatementScope, functionMapper: FunctionMapper<CExpression>): List<CExpression> {
        return when (arg) {
            is StringLiteral -> {
                if (arg.value.contains('%')) {
                    listOf(CString("%s\n"), CString(arg.value))
                } else {
                    listOf(CString(arg.value + "\n"))
                }
            }

            is IntLiteral -> listOf(CString(arg.value.toString() + "\n"))
            is BooleanLiteral -> listOf(CString(arg.value.toString() + "\n"))
            is VariableReference -> toPrintfArgs(arg, CVariableReference(arg))
            is InfixFunctionCall -> toPrintfArgs(arg, mapInfix(arg, scope, functionMapper))
            is FunctionCallExpression -> listOf(CString("%s\n"), functionMapper.map(arg))
            is ReceiverFunctionCallExpression -> listOf(CString("%s\n"), mapFunctionCall(arg, scope, functionMapper))
        }
    }

    private fun toPrintfArgs(expression: Expression, cExpression: CExpression): List<CExpression> {
        return when (expression.type) {
            is Type.String -> listOf(CString("%s\n"), cExpression)
            is Type.File -> listOf(CString("File(%s)\n"), cExpression)
            is Type.Int -> listOf(CString("%d\n"), cExpression)
            is Type.Boolean -> listOf(CTernaryExpression(cExpression, CString("${BooleanLiteral.TRUE}\n"), CString("${BooleanLiteral.FALSE}\n")))
        }
    }

    private fun SourceGenerator.append(declaration: CVariableDeclaration) {
        append("  ")
        when (declaration.type) {
            is Type.String, Type.File -> append("char *")
            is Type.Int -> append("int32_t")
            is Type.Boolean -> append("bool")
        }
        append(" ")
        append(declaration.name)
        append(" = ")
        append(declaration.value)
        appendln(";")
    }

    private fun SourceGenerator.append(statement: CFunctionCall) {
        append("  ")
        appendCall(statement)
        appendln(";")
    }

    private fun SourceGenerator.appendCall(statement: CFunctionCall) {
        append(statement.name)
        append('(')
        for (index in statement.arguments.indices) {
            val argument = statement.arguments[index]
            if (index > 0) {
                append(", ")
            }
            append(argument)
        }
        append(")")
    }

    private fun SourceGenerator.append(expression: CExpression) {
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
                append(expression.condition)
                append("?")
                append(expression.left)
                append(":")
                append(expression.right)
            }

            is CInfixOperator -> {
                append(expression.left)
                append(' ')
                append(expression.operator)
                append(' ')
                append(expression.right)
            }

            is CFunctionCall -> {
                appendCall(expression)
            }

            is CPreCalculatedExpression -> {
                append(expression.value.name)
            }
        }
    }
}