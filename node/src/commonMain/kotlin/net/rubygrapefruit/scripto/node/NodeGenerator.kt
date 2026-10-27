package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.generator.writeTo
import net.rubygrapefruit.scripto.ir.Expression
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.ir.StandardLibrary
import net.rubygrapefruit.scripto.ir.StringLiteral

class NodeGenerator {
    fun generate(program: Program, output: Directory): RegularFile {
        val operationMapper = OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                JsFunctionCall("console.log", emptyList())
            }
            map(StandardLibrary.printlnOneArg) { operation ->
                val arguments = operation.arguments.map { toExpression(it) }
                JsFunctionCall("console.log", arguments)
            }
        }

        val statements = operationMapper.map(program.operations)

        val outputFile = output.file("${program.snakeCaseName}.js")
        writeTo(outputFile, "//") {
            for (statement in statements) {
                append(statement)
            }
        }
        return outputFile
    }

    private fun toExpression(expression: Expression): JsExpression {
        return when (expression) {
            is StringLiteral -> JsString(expression.value)
            else -> JsString("??")
        }
    }
}