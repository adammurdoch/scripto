package net.rubygrapefruit.scripto.compiler

import de.infix.testBalloon.framework.core.testSuite

val GenericFunctionsTest by testSuite {
    withCompiler {
        test("fails when list literal arguments do not all have the same type") {
            sourceFile.writeText(
                """
val l1 = ["string", 12, false, []]
val l2 = List(4, "string")
        """.trimIndent()
            )

            compileFails(listOf("Line 1: 11: All arguments must have the same type", "Line 2: 15: All arguments must have the same type"))
        }
    }
}