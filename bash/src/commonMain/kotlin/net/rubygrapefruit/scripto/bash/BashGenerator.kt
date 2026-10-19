package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.file.PosixPermissions
import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.generator.SourceGenerator
import net.rubygrapefruit.scripto.generator.StatementScope
import net.rubygrapefruit.scripto.generator.writeTo
import net.rubygrapefruit.scripto.ir.*

class BashGenerator {
    fun generate(program: Program, outputFile: RegularFile) {
        val scope = StatementScope()

        val operationMapper = createOperationMapper(scope)

        val statements = operationMapper.map(program.operations)
        val bashProgram = BashProgram(statements)

        writeTo(outputFile) {
            appendln("# generated file")
            appendln()
            for (statement in bashProgram.statements) {
                append(statement)
            }
        }
        outputFile.setPermissions(PosixPermissions.readWriteFile.executable())
    }

    private fun createOperationMapper(scope: StatementScope): OperationMapper<BashStatement> {
        return OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                BashCommandInvocation("echo", emptyList())
            }
            map(StandardLibrary.printlnOneArg) { call ->
                val args = call.arguments.map { toExpression(it, scope) }
                BashCommandInvocation("echo", args)
            }
            map { declaration ->
                val value = declaration.value
                BashVariableAssignment(declaration.name, toExpression(value, scope))
            }
            collectDependencies { statement: BashStatement ->
                addDependencies(statement)
            }
        }
    }

    private fun toExpression(expression: Expression, scope: StatementScope): BashExpression {
        return when (expression) {
            is StringLiteral -> BashString(expression.value)
            is IntLiteral -> BashWord(expression.value.toString())
            is BooleanLiteral -> BashWord(expression.value.toString())
            is VariableReference -> varReferenceToBash(expression)
            is FunctionCallExpression -> {
                when (expression.target) {
                    StandardLibrary.fileConstructor -> toExpression(expression.args.first(), scope)
                    StandardLibrary.intAddition -> BashArithmetic(toExpression(expression.args[0], scope), "+", toExpression(expression.args[1], scope))
                    StandardLibrary.stringConcatenation -> BashConcatenatedExpressions(listOf(toExpression(expression.args[0], scope), toExpression(expression.args[1], scope)))
                    StandardLibrary.intEquality, StandardLibrary.booleanEquality, StandardLibrary.stringEquality -> {
                        val test = listOf(toExpression(expression.args[0], scope), BashWord("="), toExpression(expression.args[1], scope))
                        val trueBranch = BashVariableAssignment("TMP_VAR", BashWord("true"))
                        val falseBranch = BashVariableAssignment("TMP_VAR", BashWord("false"))
                        val statement = BashIfStatement(test, trueBranch, falseBranch)
                        BashCommandInvocationExpression("TMP_VAR", statement)
                    }

                    else -> {
                        throw IllegalArgumentException("Unknown function '${expression.target}'")
                    }
                }
            }

            is ReceiverFunctionCallExpression -> {
                if (expression.target == StringFunctions.toString) {
                    toExpression(expression.receiver, scope)
                } else if (expression.target == IntFunctions.toString) {
                    toExpression(expression.receiver, scope)
                } else if (expression.target == BooleanFunctions.toString) {
                    toExpression(expression.receiver, scope)
                } else if (expression.target == FileFunctions.toString) {
                    BashConcatenatedExpressions(listOf(BashString("File("), toExpression(expression.receiver, scope), BashString(")")))
                } else if (expression.target == FileFunctions.readText) {
                    val varName = scope.newVarName("READ_TEXT")
                    val readText = readFileIntoVar(toExpression(expression.receiver, scope), varName)
                    BashCommandInvocationExpression(varName, readText)
                } else {
                    throw IllegalArgumentException("Unknown ${expression.target.displayName} for ${expression.receiver.type}")
                }
            }
        }
    }

    private fun varReferenceToBash(expression: VariableReference): BashWord {
        return when (expression.type) {
            Type.String -> BashWord("\"\$${expression.target.name}\"")
            Type.File -> BashWord("\"File(\$${expression.target.name})\"")
            Type.Int, Type.Boolean -> BashWord("\$${expression.target.name}")
        }
    }

    private fun readFileIntoVar(file: BashExpression, varName: String): BashCommandInvocation {
        return BashCommandInvocation("IFS= read -rd '' $varName", emptyList(), redirectInput = file)
    }

    private fun SourceGenerator.append(statement: BashStatement) {
        when (statement) {
            is BashVariableAssignment -> append(statement)
            is BashCommandInvocation -> append(statement)
            is BashIfStatement -> append(statement)
        }
    }

    private fun SourceGenerator.append(declaration: BashVariableAssignment) {
        append(declaration.name)
        append("=")
        append(declaration.value)
        appendln()
    }

    private fun SourceGenerator.append(statement: BashCommandInvocation) {
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