package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractIntIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles variable defintion initialized with interface literal`() {
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
    fun `compiles multiple variable defintions initialized with int literals`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = 1
val b = 2
val c = 3
val d = 4
val e = 5 // more than 4 to cover JVM bytecode
println(a)
println(b)
println(c)
println(d)
println(e)
        """
        )

        compileAndRun(sourceFile) {
            statements(10)
            output(
                """1
2
3
4
5
"""
            )
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
    fun `compiles call with int literal argument`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(40)
        """
        )

        compileAndRun(sourceFile) {
            statements(1)
            output("40\n")
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

    @Test
    fun `compiles multiple calls to int variable toString() function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = -123
val b = 900
println(a.toString())
println(b.toString())
        """
        )

        compileAndRun(sourceFile) {
            output("-123\n900\n")
        }
    }

    @Test
    fun `compiles variable initialized with result of int literal toString()`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = -123.toString()
println(a)
        """
        )

        compileAndRun(sourceFile) {
            output("-123\n")
        }
    }

    @Test
    fun `compiles variable initialized with result of int variable toString()`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = -123
val b = a.toString()
println(b)
        """
        )

        compileAndRun(sourceFile) {
            output("-123\n")
        }
    }

}