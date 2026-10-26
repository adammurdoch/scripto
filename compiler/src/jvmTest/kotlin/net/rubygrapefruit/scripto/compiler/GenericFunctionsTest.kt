package net.rubygrapefruit.scripto.compiler

import de.infix.testBalloon.framework.core.testSuite
import net.rubygrapefruit.scripto.ir.FunctionCallExpression
import net.rubygrapefruit.scripto.ir.Type
import net.rubygrapefruit.scripto.ir.VariableDeclaration
import kotlin.test.assertEquals

val GenericFunctionsTest by testSuite {
    withCompiler {
        testSuite("fails when list literal arguments do not all have the same type") {
            val candidates = listOf(
                "\"string\", 12, false, []" to "String, Int, Boolean, List<Any>",
                "4, \"string\"" to "Int, String",
                "[\"string\"], [[\"string\"]]" to "List<String>, List<List<String>>"
            )
            for (candidate in candidates) {
                test("elements ${candidate.first}") {
                    sourceFile.writeText(
                        """
val l1 = [${candidate.first}]
val l2 = List(${candidate.first})
        """.trimIndent()
                    )

                    compileFails(
                        listOf(
                            "Line 1: 11: All arguments must have the same type, found: ${candidate.second}",
                            "Line 2: 15: All arguments must have the same type, found: ${candidate.second}"
                        )
                    )
                }
            }
        }

        testSuite("empty list has every list type") {
            val candidates = listOf(
                "[1], [], [2]" to Type.List(Type.Int),
                "[[\"a\"]], [[\"b\"]], []" to Type.List(Type.List(Type.String)),
                "[], [true]" to Type.List(Type.Boolean),
                "[], [true], []" to Type.List(Type.Boolean),
                "[], []" to Type.List(Type.Any)
            )
            for (candidate in candidates) {
                test("elements ${candidate.first}") {
                    sourceFile.writeText(
                        """
val l0 = [${candidate.first}]
        """.trimIndent()
                    )
                    val program = compile()
                    val functionCall = (program.operations[0] as VariableDeclaration).value as FunctionCallExpression
                    val listType = functionCall.target.returnType
                    assertEquals(Type.List(candidate.second), listType)
                    for (arg in functionCall.args) {
                        assertEquals(candidate.second, arg.type)
                    }
                }
            }
        }
    }
}