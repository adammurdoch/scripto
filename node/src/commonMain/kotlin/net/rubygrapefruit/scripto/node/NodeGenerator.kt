package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.FunctionMapper
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.generator.writeTo
import net.rubygrapefruit.scripto.ir.*

class NodeGenerator {
    fun generate(program: Program, output: Directory): RegularFile {
        val functionMapper = FunctionMapper.of {
            mapReceiver(StringFunctions.toString) { expression, functionMapper ->
                toExpression(expression.receiver, functionMapper)
            }
            mapReceiver(IntFunctions.toString) { expression, functionMapper ->
                toExpression(expression.receiver, functionMapper)
            }
            mapReceiver(BooleanFunctions.toString) { expression, functionMapper ->
                toExpression(expression.receiver, functionMapper)
            }
        }
        val operationMapper = createOperationMapper(functionMapper)

        val statements = operationMapper.map(program.operations)

        val outputFile = output.file("${program.snakeCaseName}.js")
        writeTo(outputFile, "//") {
            for (statement in statements) {
                append(statement)
            }
        }
        return outputFile
    }

    private fun createOperationMapper(functionMapper: FunctionMapper<JsExpression>): OperationMapper<JsStatement> = OperationMapper.of {
        map(StandardLibrary.printlnNoArgs) {
            JsFunctionCall("console.log", emptyList())
        }
        map(StandardLibrary.printlnOneArg) { operation ->
            val arguments = operation.arguments.map { toExpression(it, functionMapper) }
            JsFunctionCall("console.log", arguments)
        }
        map { variable ->
            JsVariableDeclaration(variable.name, toExpression(variable.value, functionMapper))
        }
    }

    private fun toExpression(expression: Expression, functionMapper: FunctionMapper<JsExpression>): JsExpression {
        return when (expression) {
            is StringLiteral -> JsString(expression.value)
            is IntLiteral -> JsInt(expression.value)
            is BooleanLiteral -> JsBoolean(expression.value)
            is VariableReference -> JsVariableReference(expression.name)
            is FunctionCallExpression -> functionMapper.map(expression)
            is ReceiverFunctionCallExpression -> functionMapper.map(expression)
        }
    }
}