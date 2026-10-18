package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractIntAdditionIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles function with int addition argument`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(10 + 20)
println(0 + 1) // cover JVM bytecode
println(2 + 3) // cover JVM bytecode
println(-1 + 4) // cover JVM bytecode
println(5 + 6) // cover JVM bytecode
        """
        )

        compileAndRun(sourceFile) {
            statements(5)
            output(
                """30
1
5
3
11
"""
            )
        }
    }

    @Test
    fun `compiles function with chained int addition argument`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(1 + 2 + 10 + 20)
        """
        )

        compileAndRun(sourceFile) {
            statements(1)
            output("33\n")
        }
    }

    @Test
    fun `compiles variable declaration initialized with int addition`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val sum = 2 + 4
println(sum)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("6\n")
        }
    }

    @Test
    fun `compiles int addition with variable references as arguments`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = 2
val b = 4
println(a + b)
        """
        )

        compileAndRun(sourceFile) {
            statements(3)
            output("6\n")
        }
    }

}