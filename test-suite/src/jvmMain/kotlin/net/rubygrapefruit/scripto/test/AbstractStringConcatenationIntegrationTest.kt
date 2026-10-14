package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractStringConcatenationIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles string concatenation argument to stdlib function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println("ab" + "c")
println("" + "abc")
println("" + "")
        """
        )

        compileAndRun(sourceFile) {
            statements(3)
            output("abc\nabc\n\n")
        }
    }

    @Test
    fun `compiles variable declaration with string concatenation initializer`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val s = "ab" + "c"
println(s)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("abc\n")
        }
    }

    @Test
    fun `compiles string concatenation with variable references as arguments`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = "ab"
val b = "c"
println(a + b)
        """
        )

        compileAndRun(sourceFile) {
            statements(3)
            output("abc\n")
        }
    }

}