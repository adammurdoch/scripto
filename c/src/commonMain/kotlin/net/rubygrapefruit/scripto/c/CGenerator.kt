package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.writeTo
import net.rubygrapefruit.scripto.ir.*

class CGenerator {
    fun generate(program: Program, outputFile: RegularFile) {
        val scope = CStatementScope()

        val statements = CStatementBuilder()
        mapOperations(program.operations, statements, scope)

        val cProgram = CProgram(statements.statements)
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

    private fun mapOperations(
        operations: List<Operation>,
        statements: CStatementBuilder,
        scope: CStatementScope
    ) {
        for (operation in operations) {
            when (operation) {
                is FunctionCall -> {
                    when (operation.target) {
                        StandardLibrary.printlnNoArgs -> {
                            statements.add(CFunctionCall("puts", listOf(CString("")), Header.Stdio))
                        }

                        StandardLibrary.printlnOneArg -> {
                            val arg = operation.arguments.first()
                            when (arg.type) {
                                is Type.String -> {
                                    statements.add(CFunctionCall("puts", listOf(toExpression(arg, scope)), Header.Stdio))
                                }

                                else -> {
                                    val args = toPrintfArgs(arg, scope)
                                    statements.add(CFunctionCall("printf", args, Header.Stdio))
                                }
                            }
                        }
                    }
                }

                is VariableDeclaration -> {
                    val value = operation.value
                    val initializer = toExpression(value, scope)
                    val header = if (operation.type is Type.Boolean) Header.Stdbool else null
                    statements.add(CVariableDeclaration(operation, initializer, header))
                }
            }
        }
    }

    private fun toExpression(expression: Expression, scope: CStatementScope): CExpression {
        return when (expression) {
            is StringLiteral -> CString(expression.value)
            is IntLiteral -> CInt(expression.value)
            is BooleanLiteral -> if (expression.value) CTrue else CFalse
            is VariableReference -> CVariableReference(expression)
            is InfixFunctionCall -> mapInfix(expression, scope)
            is FunctionCallExpression -> mapFunctionCall(expression, scope)
            is ReceiverFunctionCallExpression -> mapFunctionCall(expression, scope)
        }
    }

    private fun mapFunctionCall(expression: FunctionCallExpression, scope: CStatementScope): CExpression {
        return when (expression.target) {
            StandardLibrary.readText -> CFunctionCall("readText", expression.args.map { toExpression(it, scope) }, SourceFragment.ReadText)
            StandardLibrary.fileConstructor -> toExpression(expression.args.first(), scope)
            else -> throw IllegalArgumentException("Unknown function '${expression.target.name}'")
        }
    }

    private fun mapFunctionCall(expression: ReceiverFunctionCallExpression, scope: CStatementScope): CExpression {
        return if (expression.target == StringFunctions.toString) {
            toExpression(expression.receiver, scope)
        } else if (expression.target == FileFunctions.toString) {
            concatString(CString("File("), concatString(toExpression(expression.receiver, scope), CString(")")))
        } else if (expression.target == IntFunctions.toString) {
            val receiver = expression.receiver
            when (receiver) {
                is IntLiteral -> CString(receiver.value.toString())
                else -> {
                    val value = toExpression(expression.receiver, scope)
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
                        stringVar,
                        emptyList()
                    )
                }
            }
        } else if (expression.target == BooleanFunctions.toString) {
            val receiver = expression.receiver
            when (receiver) {
                is BooleanLiteral -> CString(receiver.value.toString())
                else -> {
                    val value = toExpression(expression.receiver, scope)
                    CTernaryExpression(value, CString(BooleanLiteral.TRUE), CString(BooleanLiteral.FALSE))
                }
            }
        } else {
            throw IllegalArgumentException("Unknown function '${expression.receiver.type}.${expression.target.name}()'")
        }
    }

    private fun mapInfix(expression: InfixFunctionCall, scope: CStatementScope): CExpression {
        return when (expression.target) {
            StandardLibrary.stringConcatenation -> {
                val left = toExpression(expression.left, scope)
                val right = toExpression(expression.right, scope)
                concatString(left, right)
            }

            StandardLibrary.stringEquality -> {
                val left = toExpression(expression.left, scope)
                val right = toExpression(expression.right, scope)
                CInfixOperator(
                    CFunctionCall("strcmp", listOf(left, right), Header.String),
                    "==",
                    CInt(0)
                )
            }

            else -> {
                val left = toExpression(expression.left, scope)
                val right = toExpression(expression.right, scope)
                CInfixOperator(left, expression.operator, right)
            }
        }
    }

    private fun concatString(left: CExpression, right: CExpression): CExpression {
        return CFunctionCall("stringConcat", listOf(left, right), SourceFragment.StringConcatenation)
    }

    private fun toPrintfArgs(arg: Expression, scope: CStatementScope): List<CExpression> {
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
            is InfixFunctionCall -> toPrintfArgs(arg, mapInfix(arg, scope))
            is FunctionCallExpression -> listOf(CString("%s\n"), mapFunctionCall(arg, scope))
            is ReceiverFunctionCallExpression -> listOf(CString("%s\n"), mapFunctionCall(arg, scope))
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