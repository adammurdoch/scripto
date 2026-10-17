package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractStdoutIoIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles call to println function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println()
        """
        )

        compileAndRun(sourceFile) {
            statements(1)
            output("\n")
        }
    }

    @Test
    fun `compiles call to println function with string literal argument`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            $$"""
println("string")
println("")
println("white space")
println("unicode: 食べる")
println("%s")
println("$")
println("$var")
println("shell \\ ' ` !text")
        """
        )

        compileAndRun(sourceFile) {
            statements(8)
            output($$"string\n\nwhite space\nunicode: 食べる\n%s\n$\n$var\nshell \\\\ ' ` !text\n")
        }
    }

    @Test
    fun `compiles call to println function with empty string variable argument`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val s = ""
println(s)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("\n")
        }
    }

    @Test
    fun `compiles call to println function with string variable argument`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val s = "value"
println(s)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("value\n")
        }
    }

    @Test
    fun `compiles call to println function with int literal argument`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(123)
println(0)
println(1)
println(-1)
println(${Int.MAX_VALUE})
println(${Int.MIN_VALUE})
        """
        )

        compileAndRun(sourceFile) {
            statements(6)
            output(
                """123
0
1
-1
${Int.MAX_VALUE}
${Int.MIN_VALUE}
"""
            )
        }
    }

    @Test
    fun `compiles call to println function with int variable argument`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val s = 12
println(s)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("12\n")
        }
    }

    @Test
    fun `compiles call to println function with boolean literal argument`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(true)
println(false)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("true\nfalse\n")
        }
    }

    @Test
    fun `compiles call to println function with boolean variable argument`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val t = true
val f = false
println(t)
println(f)
        """
        )

        compileAndRun(sourceFile) {
            statements(4)
            output("true\nfalse\n")
        }
    }
}