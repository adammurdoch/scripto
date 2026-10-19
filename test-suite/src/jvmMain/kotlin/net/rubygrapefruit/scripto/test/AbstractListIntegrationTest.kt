package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractListIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles list literal of string literals`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = ["a", "some text"]
println(l)
        """
        )
        compileAndRun(sourceFile) {
            output("[a, some text]\n")
        }
    }

    @Test
    fun `compiles list literal of string values`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val s = "some text"
val l = ["a", s, 123.toString()]
println(l)
        """
        )
        compileAndRun(sourceFile) {
            output("[a, some text, 123]\n")
        }
    }

    @Test
    fun `compiles list constructor call with string literal arguments`() {
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
    fun `compiles empty list literal`() {
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
    fun `compiles list literal of int literals`() {
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
    fun `compiles list constructor call with int literal arguments`() {
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
    fun `compiles list literal of boolean literals`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [true, false]
println(l)
        """
        )
        compileAndRun(sourceFile) {
            output("[1, 2, -123]\n")
        }
    }

    @Test
    fun `compiles list literal of list literals`() {
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