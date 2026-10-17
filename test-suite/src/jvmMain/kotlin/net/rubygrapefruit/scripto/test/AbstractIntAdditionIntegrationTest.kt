package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractIntAdditionIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles function with int addition argument`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(2 + 4)
        """
        )

        compileAndRun(sourceFile) {
            statements(1)
            output("6\n")
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