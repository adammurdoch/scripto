package net.rubygrapefruit.scripto.compiler

import de.infix.testBalloon.framework.core.testSuite
import net.rubygrapefruit.scripto.ir.VariableDeclaration
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Covers: variable declarations and variable references in expressions.
 */
val VariablesTest by testSuite {
    withCompiler {
        testSuite("parses variable names") {
            val candidates = listOf("a", "abc", "a1", "theVar", "the_var", "A1", "TheVar")
            for (candidate in candidates) {
                test("name '$candidate'") {
                    sourceFile.writeText(
                        """
val $candidate = "string"
            """
                    )

                    val program = compile()
                    assertEquals(1, program.operations.size)

                    val operation = program.operations.first()
                    assertIs<VariableDeclaration>(operation)

                    assertEquals(candidate, operation.name)
                }
            }
        }

        test("fails when unknown variable used as function argument") {
            sourceFile.writeText(
                """
println(unknown)
        """.trimIndent()
            )

            compileFails("Line 1: 9: Unknown variable 'unknown'")
        }

        test("fails when unknown variable used as variable initializer") {
            sourceFile.writeText(
                """
val other = b
        """.trimIndent()
            )

            compileFails("Line 1: 13: Unknown variable 'b'")
        }

        test("fails when unknown variable used in list literal") {
            sourceFile.writeText(
                """
val other = [1, b]
        """.trimIndent()
            )

            compileFails("Line 1: 17: Unknown variable 'b'")
        }

        test("fails when duplicate variable declared") {
            sourceFile.writeText(
                """
val a = 1
val a = 2
        """.trimIndent()
            )

            compileFails("Line 2: 5: Variable 'a' declared multiple times")
        }

        test("cannot shadow implicit args variable") {
            sourceFile.writeText(
                """
val args = 1
        """.trimIndent()
            )

            compileFails("Line 2: 5: Variable 'args' declared multiple times")
        }
    }
}