package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractFileIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles variable initialized with call to File constructor`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val t = File("some file.txt")
println(t)
        """
        )
        compileAndRun(sourceFile) {
            output("File(some file.txt)\n")
        }
    }

    @Test
    fun `compiles function call with File constructor argument`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
println(File("some file.txt"))
        """
        )
        compileAndRun(sourceFile) {
            output("File(some file.txt)\n")
        }
    }

    @Test
    fun `compiles call to File toString()`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val t = File("some file.txt").toString()
println(t)
        """
        )
        compileAndRun(sourceFile) {
            output("File(some file.txt)\n")
        }
    }
}