package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractIntListIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles variable initialized with literal of int literals`() {
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
    fun `compiles variable with List constructor with int literal arguments`() {
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