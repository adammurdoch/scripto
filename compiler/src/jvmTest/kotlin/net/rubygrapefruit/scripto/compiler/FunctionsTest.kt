package net.rubygrapefruit.scripto.compiler

import de.infix.testBalloon.framework.core.testSuite

/**
 * Covers: Function declarations and function calls.
 */
val FunctionsTest by testSuite {
    withCompiler {
        test("fails when unknown function called") {
            sourceFile.writeText(
                """
unknown()
        """.trimIndent()
            )

            compileFails("Line 1: 1: Unknown function unknown()")
        }

        test("fails when incorrect number of arguments provided to function") {
            sourceFile.writeText(
                """
println("a", "b")
        """.trimIndent()
            )

            compileFails("Line 1: 1: Incorrect number of parameters for function println()")
        }

        test("fails when unit function used in expression") {
            sourceFile.writeText(
                """
val s = println("text")
        """.trimIndent()
            )

            compileFails("Line 1: 9: Function println() has no return value")
        }

        test("fails when unknown function called on receiver") {
            sourceFile.writeText(
                """
val s = "text".unknown()
        """.trimIndent()
            )

            compileFails("Line 1: 16: Unknown function String.unknown()")
        }

        test("fails when incorrect number of arguments provided to receiver function") {
            sourceFile.writeText(
                """
val s = "text".toString(1, 2)
        """.trimIndent()
            )

            compileFails("Line 1: 16: Incorrect number of parameters for function String.toString()")
        }

        test("fails when left argument of int addition operator has incorrect type") {
            sourceFile.writeText(
                """
println(false + 2)
        """.trimIndent()
            )

            compileFails("Line 1: 9: Argument is not of type Int")
        }

        test("fails when right argument of int addition operator has incorrect type") {
            sourceFile.writeText(
                """
println(2 + "string")
        """.trimIndent()
            )

            compileFails("Line 1: 13: Argument is not of type Int")
        }
    }
}