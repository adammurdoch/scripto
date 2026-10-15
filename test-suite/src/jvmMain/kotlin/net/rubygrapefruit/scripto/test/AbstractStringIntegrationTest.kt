package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractStringIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles string variable defintion`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val s = "string"
println(s)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("string\n")
        }
    }

    @Test
    fun `compiles call to string literal toString() function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println("text".toString())
        """
        )

        compileAndRun(sourceFile) {
            output("text\n")
        }
    }

    @Test
    fun `compiles call to string variable toString() function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val string = "text"
println(string.toString())
        """
        )

        compileAndRun(sourceFile) {
            output("text\n")
        }
    }
}