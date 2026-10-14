package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractStatementIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles string variable defintion and call to println stdlib function`() {
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
    fun `compiles int variable defintion and call to println stdlib function`() {
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
    fun `compiles boolean variable defintion and call to println stdlib function`() {
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
    fun `compiles variable defintion that references another string variable and call to println stdlib function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = "string"
val b = a
println(b)
        """
        )

        compileAndRun(sourceFile) {
            statements(3)
            output("string\n")
        }
    }

    @Test
    fun `compiles variable defintion after function call`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println("before")
val a = "string"
println(a)
        """
        )

        compileAndRun(sourceFile) {
            statements(3)
            output("before\nstring\n")
        }
    }
}