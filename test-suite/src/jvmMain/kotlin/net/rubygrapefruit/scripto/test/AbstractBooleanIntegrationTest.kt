package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractBooleanIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles variable defintion initialized with boolean literal`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val b = false
println(b)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("false\n")
        }
    }

    @Test
    fun `compiles function call with boolean literal argument`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(false)
        """
        )

        compileAndRun(sourceFile) {
            statements(1)
            output("false\n")
        }
    }

    @Test
    fun `compiles call to boolean literal toString() function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(true.toString())
        """
        )

        compileAndRun(sourceFile) {
            output("true\n")
        }
    }

    @Test
    fun `compiles call to boolean variable toString() function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val v = true
println(v.toString())
        """
        )

        compileAndRun(sourceFile) {
            output("true\n")
        }
    }
}