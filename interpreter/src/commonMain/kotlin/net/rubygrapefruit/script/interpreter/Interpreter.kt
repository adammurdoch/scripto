package net.rubygrapefruit.script.interpreter

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
                        val arg = calculate(operation.arguments.first(), vars)
                        stdOut.println(arg as String)
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
            is ReceiverFunctionCallExpression -> {
                val receiver = calculate(expression.receiver, vars)
                when (expression.target) {
                    StringFunctions.toString -> receiver.toString()
                    IntFunctions.toString -> receiver.toString()
                    else -> throw IllegalArgumentException("unknown function ${expression.receiver.type}.${expression.target}")
                }
            }

            else -> TODO()
        }
    }
}