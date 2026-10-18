package net.rubygrapefruit.script.interpreter

import net.rubygrapefruit.file.FileSystem
import net.rubygrapefruit.scripto.ir.*

class Interpreter {
    fun run(program: Program, stdOut: StdOut) {
        val vars = mutableMapOf<String, Any>()
        for (operation in program.operations) {
            when (operation) {
                is VariableDeclaration -> {
                    val value = calculate(operation.value, vars)
                    vars[operation.name] = value
                }

                is FunctionCall -> {
                    if (operation.target == StandardLibrary.printlnNoArgs) {
                        stdOut.println("")
                    } else if (operation.target == StandardLibrary.printlnOneArg) {
                        val arg = operation.arguments.first()
                        val value = calculate(arg, vars)
                        val string = when(arg.type) {
                            is Type.File -> "File($value)"
                            else -> value.toString()
                        }
                        stdOut.println(string)
                    }
                }
            }
        }
    }

    private fun calculate(expression: Expression, vars: Map<String, Any>): Any {
        return when (expression) {
            is StringLiteral -> expression.value
            is IntLiteral -> expression.value
            is BooleanLiteral -> expression.value
            is VariableReference -> vars.getValue(expression.target.name)
            is FunctionCallExpression -> {
                val args = expression.args.map { calculate(it, vars) }
                when (expression.target) {
                    StandardLibrary.fileConstructor -> args.first()
                    else -> throw IllegalArgumentException("unknown function ${expression.target}")
                }
            }

            is InfixFunctionCall -> {
                val left = calculate(expression.left, vars)
                val right = calculate(expression.right, vars)
                when (expression.target) {
                    StandardLibrary.stringEquality -> left == right
                    StandardLibrary.stringConcatenation -> "$left$right"
                    StandardLibrary.intEquality -> left == right
                    StandardLibrary.intAddition -> (left as Int) + (right as Int)
                    StandardLibrary.booleanEquality -> left == right
                    else -> throw IllegalArgumentException("unknown function ${expression.target}")
                }
            }

            is ReceiverFunctionCallExpression -> {
                val receiver = calculate(expression.receiver, vars)
                when (expression.target) {
                    StringFunctions.toString -> receiver.toString()
                    IntFunctions.toString -> receiver.toString()
                    BooleanFunctions.toString -> receiver.toString()
                    FileFunctions.toString -> "File($receiver)"
                    FileFunctions.readText -> FileSystem.local.currentDirectory.file(receiver as String).readText()
                    else -> throw IllegalArgumentException("unknown function ${expression.receiver.type}.${expression.target.name}()")
                }
            }
        }
    }
}