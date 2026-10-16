package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractIntIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles int variable defintion with literal initializer`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val i = 40
println(i)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("40\n")
        }
    }

    @Test
    fun `compiles call to int literal toString() function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(123.toString())
        """
        )

        compileAndRun(sourceFile) {
            output("123\n")
        }
    }

    @Test
    fun `compiles call to int variable toString() function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val v = -123
println(v.toString())
        """
        )

        compileAndRun(sourceFile) {
            output("-123\n")
        }
    }

}