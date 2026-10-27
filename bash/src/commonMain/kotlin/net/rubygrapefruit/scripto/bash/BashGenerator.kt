package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.file.PosixPermissions
import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.*
import net.rubygrapefruit.scripto.ir.*

class BashGenerator : TargetRuntime {
    private val listToString by lazy {
        BashGeneratedFunction(
            "_list_to_string", "_list_to_string_r", $$"""
local result="["                
local separator=""
local array_ref="$1[@]"
for element in "${!array_ref}"; do
    result+="$separator$element"
    separator=", "
done
result+="]"
_list_to_string_r="$result"
            """.trimIndent()
        )
    }

    private val fileListToString by lazy {
        BashGeneratedFunction(
            "_file_list_to_string", "_file_list_to_string_r", $$"""
local result="["                
local separator=""
local array_ref="$1[@]"
for element in "${!array_ref}"; do
    result+="${separator}"File"(${element})"
    separator=", "
done
result+="]"
_file_list_to_string_r="$result"
            """.trimIndent()
        )
    }

    private var enabled = true

    override fun inspect(function: FunctionDefinition) {
        val target = function
        if (target is RealizedFunctionDefinition && target.definition == StandardLibrary.listConstructor) {
            if (target.argument is Type.List) {
                println("${target.returnType} is not supported for Bash")
                enabled = false
            }
        }
    }

    fun generate(program: Program, output: Directory): RegularFile {
        val outputFile = output.file("${program.snakeCaseName}.bash")
        if (!enabled) {
            outputFile.delete()
            return outputFile
        }

        val functionMapper = createFunctionMapper()
        val operationMapper = createOperationMapper(functionMapper)

        val statements = mutableListOf<BashStatement>()
        operationMapper.mapInto(program.operations, statements)

        val variableState = VariableState()

        val queue = Declaration.Queue<BashGeneratedFunction>()
        var index = 0
        var argsDeclared = false
        while (index < statements.size) {
            val statement = statements[index]
            statement.visitExpressions { expression ->
                expression.visitVariables { variable ->
                    if (variable is BashVariable.Temporary) {
                        variableState.assign(variable)
                    } else if (variable is BashVariable.Named && variable.name == StandardLibrary.argsVariable.name && !argsDeclared) {
                        // Should move this to a dependency
                        argsDeclared = true
                        statements.add(index, BashVariableAssignment(variable, BashWord("(\"$@\")")))
                        index++
                    }
                }
                expression.visitDeclarations { declaration ->
                    queue.add(declaration)
                }
            }
            index++
        }

        writeTo(outputFile, "#") {
            queue.visitAll { function ->
                function.run { append() }
            }

            for (statement in statements) {
                append(statement, variableState)
            }
        }
        outputFile.setPermissions(PosixPermissions.readWriteFile.executable())
        return outputFile
    }

    private fun createOperationMapper(functionMapper: FunctionMapper<BashExpression>): OperationMapper<BashStatement> {
        return OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                BashCommandInvocation("echo", emptyList())
            }
            map(StandardLibrary.printlnOneArg) { call ->
                val call = StandardLibrary.convertPrintln(call)
                val args = call.arguments.map { toExpression(it, functionMapper) }
                BashCommandInvocation("echo", args)
            }
            map { declaration ->
                val value = declaration.value
                BashVariableAssignment(BashVariable.Named(declaration.name), toExpression(value, functionMapper))
            }
            collectDependencies { statement: BashStatement ->
                statement.visitDependencies { dependency ->
                    add(dependency)
                }
            }
        }
    }

    private fun createFunctionMapper(): FunctionMapper<BashExpression> {
        return FunctionMapper.of {
            map(StandardLibrary.fileConstructor) { expression, functionMapper ->
                toExpression(expression.args.first(), functionMapper)
            }
            map(StandardLibrary.listConstructor) { expression, _, functionMapper ->
                BashArrayLiteral(expression.args.map { toExpression(it, functionMapper) })
            }
            mapOperator(StandardLibrary.intAddition) { left, right ->
                BashArithmetic(left, "+", right)
            }
            mapOperator(StandardLibrary.stringConcatenation) { left, right ->
                BashConcatenatedExpressions(listOf(left, right))
            }

            val equality = { left: BashExpression, right: BashExpression ->
                val result = BashVariable.Temporary("equal")
                val test = listOf(left, BashWord("="), right)
                val trueBranch = BashVariableAssignment(result, BashWord("true"))
                val falseBranch = BashVariableAssignment(result, BashWord("false"))
                val statement = BashIfStatement(test, trueBranch, falseBranch)
                BashCommandInvocationExpression(result, listOf(statement))
            }
            mapOperator(StandardLibrary.stringEquality, equality)
            mapOperator(StandardLibrary.intEquality, equality)
            mapOperator(StandardLibrary.booleanEquality, equality)

            mapReceiver(StringFunctions.toString) { expression, functionMapper ->
                toExpression(expression.receiver, functionMapper)
            }
            mapReceiver(IntFunctions.toString) { expression, functionMapper ->
                toExpression(expression.receiver, functionMapper)
            }
            mapReceiver(BooleanFunctions.toString) { expression, functionMapper ->
                toExpression(expression.receiver, functionMapper)
            }
            mapReceiver(FileFunctions.toString) { expression, functionMapper ->
                BashConcatenatedExpressions(listOf(BashString("File("), toExpression(expression.receiver, functionMapper), BashString(")")))
            }
            mapReceiver(ListFunctions.toString) { expression, functionMapper ->
                val receiver = toExpression(expression.receiver, functionMapper)
                val toStringFunction = when ((expression.receiver.type as Type.List).param) {
                    is Type.File -> fileListToString
                    else -> listToString
                }
                // Have to pass a variable name to the function
                val statements = when (receiver) {
                    is BashVariableReference -> listOf(
                        BashCommandInvocation(toStringFunction.name, listOf(BashVariableName(receiver.variable)))
                    )

                    else -> {
                        val tmp = BashVariable.Temporary("list")
                        listOf(
                            BashVariableAssignment(tmp, receiver),
                            BashCommandInvocation(toStringFunction.name, listOf(BashVariableName(tmp)))
                        )
                    }
                }
                BashCommandInvocationExpression(BashVariable.Named(toStringFunction.returnVar), statements, toStringFunction)
            }
            mapReceiver(FileFunctions.readText) { expression, functionMapper ->
                val result = BashVariable.Temporary("text")
                val readText = readFileIntoVar(toExpression(expression.receiver, functionMapper), result)
                BashCommandInvocationExpression(result, listOf(readText))
            }
        }
    }

    private fun FunctionMapper.Builder<BashExpression>.mapOperator(operator: OperatorDefinition, builder: (BashExpression, BashExpression) -> BashExpression) {
        map(operator) { expression, functionMapper ->
            val left = toExpression(expression.args[0], functionMapper)
            val right = toExpression(expression.args[1], functionMapper)
            builder(left, right)
        }
    }

    private fun toExpression(expression: Expression, functionMapper: FunctionMapper<BashExpression>): BashExpression {
        return when (expression) {
            is StringLiteral -> BashString(expression.value)
            is IntLiteral -> BashWord(expression.value.toString())
            is BooleanLiteral -> BashWord(expression.value.toString())
            is VariableReference -> varReferenceToBash(expression)
            is FunctionCallExpression -> functionMapper.map(expression)
            is ReceiverFunctionCallExpression -> functionMapper.map(expression)
        }
    }

    private fun varReferenceToBash(expression: VariableReference): BashExpression {
        return when (expression.type) {
            is Type.Boolean, is Type.Int -> BashVariableReference(BashVariable.Named(expression.name), false)
            else -> BashVariableReference(BashVariable.Named(expression.name), true)
        }
    }

    private fun readFileIntoVar(file: BashExpression, target: BashVariable): BashCommandInvocation {
        return BashCommandInvocation("IFS= read -rd ''", listOf(BashVariableName(target)), redirectInput = file)
    }
}