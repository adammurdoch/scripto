package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractStatementIntegrationTest : AbstractIntegrationTest() {

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
    fun `compiles chained string toString() calls`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val s = "text"
println(s.toString().toString().toString())
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("text\n")
        }
    }

    @Test
    fun `compiles chained int and string toString() calls`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val i = -500
println(i.toString().toString())
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("-500\n")
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