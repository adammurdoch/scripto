package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.file.PosixPermissions
import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.writeTo
import net.rubygrapefruit.scripto.ir.*

class BashGenerator {
    fun generate(program: Program, outputFile: RegularFile) {
        val operationMapper = OperationMapper.of {
            mapFun(StandardLibrary.printlnNoArgs) {
                BashCommandInvocation("echo", emptyList())
            }
            mapFun(StandardLibrary.printlnOneArg) {
                val args = it.arguments.map { toExpression(it) }
                BashCommandInvocation("echo", args)
            }
            mapVar { declaration ->
                val value = declaration.value
                if (value is FunctionCallExpression && value.target == StandardLibrary.readText) {
                    readFileIntoVar(value.args.first(), declaration.name)
                } else {
                    BashVariableAssignment(declaration.name, toExpression(value))
                }
            }
        }

        val bashProgram = BashProgram(operationMapper.map(program.operations))

        writeTo(outputFile) {
            appendln("# generated file")
            appendln()
            for (statement in bashProgram.statements) {
                append(statement)
            }
        }
        outputFile.setPermissions(PosixPermissions.readWriteFile.executable())
    }

    private fun toExpression(expression: Expression): BashExpression {
        return when (expression) {
            is StringLiteral -> BashString(expression.value)
            is IntLiteral -> BashWord(expression.value.toString())
            is BooleanLiteral -> BashWord(expression.value.toString())
            is VariableReference -> varReferenceToBash(expression)
            is InfixFunctionCall -> mapInfix(expression)
            is FunctionCallExpression -> {
                if (expression.target == StandardLibrary.readText) {
                    val readText = readFileIntoVar(expression.args.first(), "TMP_VAR")
                    BashCommandInvocationExpression("TMP_VAR", readText)
                } else if (expression.target == StandardLibrary.fileConstructor) {
                    toExpression(expression.args.first())
                } else {
                    throw IllegalArgumentException("Unknown function '${expression.target}'")
                }
            }

            is ReceiverFunctionCallExpression -> {
                if (expression.target == StringFunctions.toString) {
                    toExpression(expression.receiver)
                } else if (expression.target == IntFunctions.toString) {
                    toExpression(expression.receiver)
                } else {
                    throw IllegalArgumentException("Unknown function '${expression.receiver}.${expression.target}'")
                }
            }
        }
    }

    private fun mapInfix(expression: InfixFunctionCall): BashExpression {
        return when (expression.target) {
            StandardLibrary.intAddition -> BashArithmetic(toExpression(expression.left), expression.operator, toExpression(expression.right))
            StandardLibrary.stringConcatenation -> BashConcatenatedExpressions(listOf(toExpression(expression.left), toExpression(expression.right)))
            else -> {
                val test = listOf(toExpression(expression.left), BashWord("="), toExpression(expression.right))
                val trueBranch = BashVariableAssignment("TMP_VAR", BashWord("true"))
                val falseBranch = BashVariableAssignment("TMP_VAR", BashWord("false"))
                val statement = BashIfStatement(test, trueBranch, falseBranch)
                BashCommandInvocationExpression("TMP_VAR", statement)
            }
        }
    }

    private fun varReferenceToBash(expression: VariableReference): BashWord {
        return when (expression.type) {
            Type.String -> BashWord("\"\$${expression.target.name}\"")
            Type.Int, Type.Boolean -> BashWord("\$${expression.target.name}")
        }
    }

    private fun readFileIntoVar(file: Expression, varName: String): BashCommandInvocation {
        return BashCommandInvocation("IFS= read -rd '' $varName", emptyList(), redirectInput = toExpression(file))
    }

    private fun SourceGenerator.append(statement: BashStatement) {
        when (statement) {
            is BashVariableAssignment -> append(statement)
            is BashCommandInvocation -> append(statement)
            is BashIfStatement -> append(statement)
        }
    }

    private fun SourceGenerator.append(declaration: BashVariableAssignment) {
        declaration.visitStatements {
            append(it)
        }
        append(declaration.name)
        append("=")
        append(declaration.value)
        appendln()
    }

    private fun SourceGenerator.append(statement: BashCommandInvocation) {
        statement.visitStatements {
            append(it)
        }
        append(statement.command)
        for (arg in statement.args) {
            append(" ")
            append(arg)
        }
        if (statement.redirectInput != null) {
            append("<")
            append(statement.redirectInput)
        }
        appendln()
    }

    private fun SourceGenerator.append(statement: BashIfStatement) {
        append("if [ ")
        for (arg in statement.test) {
            append(arg)
            append(' ')
        }
        appendln("]; then")
        append(statement.trueBranch)
        appendln("else")
        append(statement.falseBranch)
        appendln("fi")
    }

    private fun SourceGenerator.append(expression: BashExpression) {
        when (expression) {
            is BashString -> {
                append("\"")
                for (ch in expression.value) {
                    when (ch) {
                        '`', '$', '\\' -> {
                            append("\\")
                            append(ch)
                        }

                        else -> append(ch)
                    }
                }
                append("\"")
            }

            is BashWord -> append(expression.value)

            is BashConcatenatedExpressions -> {
                for (nested in expression.values) {
                    append(nested)
                }
            }

            is BashArithmetic -> {
                append("$((")
                append(expression.left)
                append(expression.operator)
                append(expression.right)
                append("))")
            }

            is BashCommandInvocationExpression -> {
                append("\"$")
                append(expression.intermediateVariable)
                append("\"")
            }
        }
    }
}