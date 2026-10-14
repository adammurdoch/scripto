package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.writeTo
import net.rubygrapefruit.scripto.ir.*

class CGenerator {
    fun generate(program: Program, outputFile: RegularFile) {
        val mapping = OperationMapper.of {
            mapFun(StandardLibrary.printlnNoArgs) {
                CFunctionCall("puts", listOf(CString("")), Header.Stdio)
            }
            mapFun(StandardLibrary.printlnOneArg) {
                val args = toPrintfArgs(it.arguments.first())
                CFunctionCall("printf", args, Header.Stdio)
            }
            mapVar { declaration ->
                val value = declaration.value
                val initializer = toExpression(value)
                val header = if (declaration.type is Type.Boolean) Header.Stdbool else null
                CVariableDeclaration(declaration, initializer, header)
            }
        }

        val cProgram = CProgram(mapping.map(program.operations))
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

    private fun toExpression(expression: Expression): CExpression {
        return when (expression) {
            is StringLiteral -> CString(expression.value)
            is IntLiteral -> CInt(expression.value)
            is BooleanLiteral -> if (expression.value) CTrue else CFalse
            is VariableReference -> CVariableReference(expression)
            is InfixFunctionCall -> mapInfix(expression)
            is FunctionCallExpression -> mapFunctionCall(expression)
        }
    }

    private fun mapFunctionCall(expression: FunctionCallExpression): CExpression {
        if (expression.target == StandardLibrary.readText) {
            return CFunctionCall("readText", expression.args.map { toExpression(it) }, SourceFragment.ReadText)
        } else {
            throw IllegalArgumentException("Unknown function '${expression.target.name}'")
        }
    }

    private fun mapInfix(expression: InfixFunctionCall): CExpression {
        return when (expression.target) {
            StandardLibrary.stringConcatenation -> CFunctionCall(
                "stringConcat",
                listOf(toExpression(expression.left), toExpression(expression.right)),
                SourceFragment.StringConcatenation
            )

            StandardLibrary.stringEquality -> CInfixOperator(
                CFunctionCall(
                    "strcmp",
                    listOf(toExpression(expression.left), toExpression(expression.right)),
                    Header.String
                ),
                "==",
                CInt(0)
            )

            else -> CInfixOperator(toExpression(expression.left), expression.operator, toExpression((expression.right)))
        }
    }

    private fun toPrintfArgs(arg: Expression): List<CExpression> {
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
            is InfixFunctionCall -> toPrintfArgs(arg, mapInfix(arg))
            is FunctionCallExpression -> listOf(CString("%s\n"), mapFunctionCall(arg))
        }
    }

    private fun toPrintfArgs(expression: Expression, cExpression: CExpression): List<CExpression> {
        return when (expression.type) {
            is Type.String -> listOf(CString("%s\n"), cExpression)
            is Type.Int -> listOf(CString("%d\n"), cExpression)
            is Type.Boolean -> listOf(CTernaryExpression(cExpression, CString("${BooleanLiteral.TRUE}\n"), CString("${BooleanLiteral.FALSE}\n")))
        }
    }

    private fun SourceGenerator.append(declaration: CVariableDeclaration) {
        append("  ")
        when (declaration.type) {
            is Type.String -> append("char *")
            is Type.Int -> append("int32_t")
            is Type.Boolean -> append("bool")
        }
        append(" ")
        append(declaration.name)
        append("=")
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
                append(expression.operator)
                append(expression.right)
            }

            is CFunctionCall -> {
                appendCall(expression)
            }
        }
    }
}