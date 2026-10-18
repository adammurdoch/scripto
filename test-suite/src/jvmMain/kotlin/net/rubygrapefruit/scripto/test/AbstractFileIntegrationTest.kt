package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractFileIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles call to File constructor`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val t = File("file.txt")
println(t)
        """
        )
        compileAndRun(sourceFile) {
            output("file.txt\n")
        }
    }

    @Test
    fun `compiles call to File toString()`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val t = File("file.txt").toString()
println(t)
        """
        )
        compileAndRun(sourceFile) {
            output("file.txt\n")
        }
    }
}