package net.rubygrapefruit.scripto.compiler

import de.infix.testBalloon.framework.core.testSuite
import net.rubygrapefruit.scripto.ir.FunctionCallExpression
import net.rubygrapefruit.scripto.ir.Type
import net.rubygrapefruit.scripto.ir.VariableDeclaration
import kotlin.test.assertEquals

val GenericFunctionsTest by testSuite {
    withCompiler {
        test("fails when list literal arguments do not all have the same type") {
            sourceFile.writeText(
                """
val l1 = ["string", 12, false, []]
val l2 = List(4, "string")
val l3 = [["string"], [["string"]]]
        """.trimIndent()
            )

            compileFails(
                listOf(
                    "Line 1: 11: All arguments must have the same type, found: String, Int, Boolean, List<Any>",
                    "Line 2: 15: All arguments must have the same type, found: Int, String",
                    "Line 3: 12: All arguments must have the same type, found: List<String>, List<List<String>>"
                )
            )
        }

        test("empty list has every list type") {
            sourceFile.writeText(
                """
val l0 = [[1], [], [2]]
val l1 = [[["a"]], [["b"]], []]
val l2 = [[], [true]]
val l3 = [[], []]
val l4 = [[], [true], []]
        """.trimIndent()
            )

            val program = compile()
            val typeForListLiteral = { index: Int -> ((program.operations[index] as VariableDeclaration).value as FunctionCallExpression).target.returnType }
            assertEquals(Type.List(Type.List(Type.Int)), typeForListLiteral(0))
            assertEquals(Type.List(Type.List(Type.List(Type.String))), typeForListLiteral(1))
            assertEquals(Type.List(Type.List(Type.Boolean)), typeForListLiteral(2))
            assertEquals(Type.List(Type.List(Type.Any)), typeForListLiteral(3))
            assertEquals(Type.List(Type.List(Type.Boolean)), typeForListLiteral(4))
        }
    }
}