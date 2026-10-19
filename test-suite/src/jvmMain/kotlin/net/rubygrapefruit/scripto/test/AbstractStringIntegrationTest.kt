package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractStringIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles string variable defintion initialized with literal`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val s = "some text"
println(s)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("some text\n")
        }
    }

    @Test
    fun `compiles multiple string variable defintion initialized with literal`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = "a"
val b = "b"
val c = "c"
val d = "d"
val e = "e" // more than 4 to cover JVM bytecode
println(a)
println(b)
println(c)
println(d)
println(e)
        """
        )

        compileAndRun(sourceFile) {
            statements(10)
            output("""a
b
c
d
e
""")
        }
    }

    @Test
    fun `compiles call to literal toString() function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println("some text".toString())
        """
        )

        compileAndRun(sourceFile) {
            output("some text\n")
        }
    }

    @Test
    fun `compiles call to string variable toString() function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val string = "some text"
println(string.toString())
        """
        )

        compileAndRun(sourceFile) {
            output("some text\n")
        }
    }
}