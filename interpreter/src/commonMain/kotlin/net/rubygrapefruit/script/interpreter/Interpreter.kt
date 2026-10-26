package net.rubygrapefruit.script.interpreter

import net.rubygrapefruit.file.FileSystem
import net.rubygrapefruit.scripto.ir.*

class Interpreter {
    fun run(program: Program, args: List<String>, stdOut: StdOut) {
        val vars = mutableMapOf<String, Any>()
        vars[StandardLibrary.argsVariable.name] = args
        for (operation in program.operations) {
            when (operation) {
                is VariableDeclaration -> {
                    val value = calculate(operation.value, vars)
                    vars[operation.name] = value
                }

                is UnitFunctionCall -> {
                    if (operation.target == StandardLibrary.printlnNoArgs) {
                        stdOut.println("")
                    } else if (operation.target == StandardLibrary.printlnOneArg) {
                        val arg = operation.arguments.first()
                        val value = calculate(arg, vars)
                        stdOut.println(value.toString())
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
            is VariableReference -> vars.getValue(expression.name)
            is FunctionCallExpression -> {
                val args = expression.args.map { calculate(it, vars) }
                val target = expression.target
                when (target) {
                    StandardLibrary.fileConstructor -> File(args.first() as String)

                    is OperatorDefinition -> {
                        val left = calculate(expression.args[0], vars)
                        val right = calculate(expression.args[1], vars)
                        when (expression.target) {
                            StandardLibrary.stringEquality -> left == right
                            StandardLibrary.stringConcatenation -> "$left$right"
                            StandardLibrary.intEquality -> left == right
                            StandardLibrary.intAddition -> (left as Int) + (right as Int)
                            StandardLibrary.booleanEquality -> left == right
                            else -> throw IllegalArgumentException("Unknown ${target.displayName}")
                        }
                    }

                    is RealizedFunctionDefinition -> {
                        when (target.definition) {
                            StandardLibrary.listConstructor -> args // already a list
                            else -> throw IllegalArgumentException("Unknown ${target.displayName}")
                        }
                    }

                    else -> throw IllegalArgumentException("Unknown ${target.displayName}")
                }
            }

            is ReceiverFunctionCallExpression -> {
                val receiver = calculate(expression.receiver, vars)
                when (expression.target) {
                    StringFunctions.toString -> receiver.toString()
                    IntFunctions.toString -> receiver.toString()
                    BooleanFunctions.toString -> receiver.toString()
                    ListFunctions.toString -> receiver.toString()
                    FileFunctions.toString -> receiver.toString()
                    FileFunctions.readText -> (receiver as File).readText()
                    else -> throw IllegalArgumentException("Unknown ${expression.target.displayName} for ${expression.receiver.type}")
                }
            }
        }
    }

    private class File(val path: String) {
        override fun toString(): String {
            return "File($path)"
        }

        fun readText(): String {
            return FileSystem.local.currentDirectory.file(path).readText()
        }
    }
}