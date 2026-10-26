package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractFileListIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles variable initialized with literal of file constructor calls`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [File("file1.txt"), File("file2.txt")]
println(l)
        """
        )
        compileAndRun(sourceFile) {
            output("[File(file1.txt), File(file2.txt)]\n")
        }
    }
}