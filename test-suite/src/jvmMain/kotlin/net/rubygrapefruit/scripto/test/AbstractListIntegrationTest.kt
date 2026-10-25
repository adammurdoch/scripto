package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractListIntegrationTest : AbstractIntegrationTest() {
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
    fun `compiles variable initialized with List constructor with no arguments`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = List()
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
    fun `compiles list variable initialized with list literal of string list literals`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [["a", "b"], ["c"]]
println(l)
        """
        )
        compileAndRun(sourceFile, features =  listOf(Feature.ListOfList)) {
            output("[[a, b], [c]]\n")
        }
    }

    @Test
    fun `compiles list variable initialized with list literal of int list literals`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [[6, 90], [-12]]
println(l)
        """
        )
        compileAndRun(sourceFile, features =  listOf(Feature.ListOfList)) {
            output("[[6, 90], [-12]]\n")
        }
    }


    @Test
    fun `compiles multiple calls to int list toString()`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [1, 5, 10]                
println(l.toString())
println([0, -4].toString())
        """
        )
        compileAndRun(sourceFile) {
            output("[1, 5, 10]\n[0, -4]\n")
        }
    }
}