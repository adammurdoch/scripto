package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.FunctionMapper
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.generator.writeTo
import net.rubygrapefruit.scripto.ir.*

class NodeGenerator {
    fun generate(program: Program, output: Directory): RegularFile {
        val functionMapper = createFunctionMapper()
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

    private fun createFunctionMapper(): FunctionMapper<JsExpression> {
        return FunctionMapper.of {
            map(StandardLibrary.fileConstructor) { expression, functionMapper ->
                toExpression(expression.args[0], functionMapper)
            }
            mapReceiver(StringFunctions.toString) { expression, functionMapper ->
                toExpression(expression.receiver, functionMapper)
            }
            mapReceiver(IntFunctions.toString) { expression, functionMapper ->
                val receiver = toExpression(expression.receiver, functionMapper)
                JsFunctionCall(receiver, "toString", emptyList())
            }
            mapReceiver(BooleanFunctions.toString) { expression, functionMapper ->
                val receiver = toExpression(expression.receiver, functionMapper)
                JsFunctionCall(receiver, "toString", emptyList())
            }
            mapReceiver(FileFunctions.toString) { expression, functionMapper ->
                val receiver = toExpression(expression.receiver, functionMapper)
                fileToString(receiver)
            }
            mapOperator(StandardLibrary.stringEquality) { left, right ->
                JsInfix(left, "==", right)
            }
            mapOperator(StandardLibrary.stringConcatenation) { left, right ->
                JsInfix(left, "+", right)
            }
            mapOperator(StandardLibrary.intEquality) { left, right ->
                JsInfix(left, "==", right)
            }
            mapOperator(StandardLibrary.intAddition) { left, right ->
                JsInfix(left, "+", right)
            }
            mapOperator(StandardLibrary.booleanEquality) { left, right ->
                JsInfix(left, "==", right)
            }
        }
    }

    private fun FunctionMapper.Builder<JsExpression>.mapOperator(operator: OperatorDefinition, map: (JsExpression, JsExpression) -> JsExpression) {
        map(operator) { expression, functionMapper ->
            val left = toExpression(expression.args[0], functionMapper)
            val right = toExpression(expression.args[1], functionMapper)
            map(left, right)
        }
    }

    private fun createOperationMapper(functionMapper: FunctionMapper<JsExpression>): OperationMapper<JsStatement> {
        return OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                JsFunctionCall(JsVariableReference("console"), "log", emptyList())
            }
            map(StandardLibrary.printlnOneArg) { operation ->
                val arguments = operation.arguments.map { toStringExpression(it, functionMapper) }
                JsFunctionCall(JsVariableReference("console"), "log", arguments)
            }
            map { variable ->
                JsVariableDeclaration(variable.name, toExpression(variable.value, functionMapper))
            }
        }
    }

    private fun toStringExpression(expression: Expression, functionMapper: FunctionMapper<JsExpression>): JsExpression {
        val value = toExpression(expression, functionMapper)
        return when (expression.type) {
            is Type.File -> fileToString(value)
            else -> value
        }
    }

    private fun fileToString(value: JsExpression): JsExpression {
        return JsInfix(JsString("File("), "+", JsInfix(value, "+", JsString(")")))
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