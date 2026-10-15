package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractStringIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles call to string variable toString() function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val string = "string"
println(string.toString())
        """
        )

        compileAndRun(sourceFile) {
            output("string\n")
        }
    }
}