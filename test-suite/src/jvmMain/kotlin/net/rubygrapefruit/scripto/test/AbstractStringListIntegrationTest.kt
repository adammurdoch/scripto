package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractStringListIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles variable initialized with list literal of string literals`() {
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
    fun `compiles variable initialized with list literal of string values`() {
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
    fun `compiles variable initialized with List constructor with string literal arguments`() {
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

    @Test
    fun `compiles call to string list variable toString()`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = ["a", "some text"]
val t = l.toString()
println(t)
        """
        )
        compileAndRun(sourceFile) {
            output("[a, some text]\n")
        }
    }
}