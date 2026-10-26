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
    fun `compiles variable initialized with list literal of string list literals`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [["a", "b"], ["c"]]
println(l)
        """
        )
        compileAndRun(sourceFile, features = listOf(Feature.ListOfList)) {
            output("[[a, b], [c]]\n")
        }
    }

    @Test
    fun `compiles variable initialized with list literal of int list literals`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [[6, 90], [-12]]
println(l)
        """
        )
        compileAndRun(sourceFile, features = listOf(Feature.ListOfList)) {
            output("[[6, 90], [-12]]\n")
        }
    }

    @Test
    fun `compiles variable initialized with list literal of boolean list literals`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [[true, false], [true]]
println(l)
        """
        )
        compileAndRun(sourceFile, features = listOf(Feature.ListOfList)) {
            output("[[true, false], [true]]\n")
        }
    }

    @Test
    fun `compiles variable initialized with list literal of nested string lists`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [[["a", "b"], ["c"]], [["d"]]]
println(l)
        """
        )
        compileAndRun(sourceFile, features = listOf(Feature.ListOfList)) {
            output("[[[a, b], [c]], [[d]]]\n")
        }
    }

    @Test
    fun `compiles variable initialized with list literal of string list literals and empty lists`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [["a", "b"], [], ["c"]]
println(l)
        """
        )
        compileAndRun(sourceFile, features = listOf(Feature.ListOfList)) {
            output("[[a, b], [], [c]]\n")
        }
    }

    @Test
    fun `compiles variable initialized with list literal of empty lists`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [[], [], []]
println(l)
        """
        )
        compileAndRun(sourceFile, features = listOf(Feature.ListOfList)) {
            output("[[], [], []]\n")
        }
    }
}