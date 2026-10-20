package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractListIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles list variable initialized with literal of string literals`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l1 = ["a", "some text"]
println(l1)
val l2 = ["some text"] // single element for PowerShell generation
println(l2)
        """
        )
        compileAndRun(sourceFile) {
            output("[a, some text]\n[some text]\n")
        }
    }

    @Test
    fun `compiles list variable initialized with literal of string values`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val s = "some text"
val l = ["a", s, 123.toString(), "a" + "bc"]
println(l)
        """
        )
        compileAndRun(sourceFile) {
            output("[a, some text, 123, abc]\n")
        }
    }

    @Test
    fun `compiles function call with list literal argument of string literals`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
println(["a", "some text"])
println(["some text"]) // single element for PowerShell generation
        """
        )
        compileAndRun(sourceFile) {
            output("[a, some text]\n[some text]\n")
        }
    }

    @Test
    fun `compiles list variable initialized with List constructor argument with string literal arguments`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = List("a", "some text")
println(l)
        """
        )
        compileAndRun(sourceFile) {
            output("[a, some text]\n")
        }
    }

    @Test
    fun `compiles list variable initialized with empty list literal`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = []
println(l)
        """
        )
        compileAndRun(sourceFile) {
            output("[]\n")
        }
    }

    @Test
    fun `compiles list variable initialized with literal of int literals`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [1, 2, -123]
println(l)
        """
        )
        compileAndRun(sourceFile) {
            output("[1, 2, -123]\n")
        }
    }

    @Test
    fun `compiles list variable with List constructor argument with int literal arguments`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = List(1, -50)
println(l)
        """
        )
        compileAndRun(sourceFile) {
            output("[1, -50]\n")
        }
    }

    @Test
    fun `compiles list variable initialized with literal of boolean literals`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [true, false]
println(l)
        """
        )
        compileAndRun(sourceFile) {
            output("[true, false]\n")
        }
    }

    @Test
    fun `compiles list variable initialized with list literal of list literals`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [["a", "b"], ["c"]]
println(l)
        """
        )
        compileAndRun(sourceFile) {
            output("[[a, b], [c]]\n")
        }
    }

    @Test
    fun `compiles call to string list literal toString()`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
println(["a", "some text"].toString())
        """
        )
        compileAndRun(sourceFile) {
            output("[a, some text]\n")
        }
    }
}