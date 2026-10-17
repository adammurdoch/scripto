package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractStringConcatenationIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles function with string concatenation argument`() {
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
    fun `compiles variable declaration initialized with string concatenation`() {
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

    @Test
    fun `compiles string concatenation with function calls as arguments`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = 12
val b = 3
println(a.toString() + b.toString())
        """
        )

        compileAndRun(sourceFile) {
            statements(3)
            output("123\n")
        }
    }

    @Test
    fun `compiles variable declaration initialized with string concatenation with function call  arguments`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = 12
val b = -3
val c = a.toString() + b.toString()
println(c)
        """
        )

        compileAndRun(sourceFile) {
            statements(4)
            output("12-3\n")
        }
    }

}