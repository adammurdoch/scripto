package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractFileIoIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles call to readText function`() {
        val sourceFile = workDir.file("app.scripto")
        val textFile = workDir.file("file.txt")
        textFile.writeText("string")

        sourceFile.writeText(
            """
val t = readText("${textFile.absolutePath}")
println(t)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("string\n")
        }
    }

    @Test
    fun `reads file with leading and trailing end line characters`() {
        val sourceFile = workDir.file("app.scripto")
        val textFile = workDir.file("file.txt")

        val text = "\n  string\n\n  123\n"
        textFile.writeText(text)

        sourceFile.writeText(
            """
val t = readText("${textFile.absolutePath}")
println(t)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("$text\n")
        }
    }

    @Test
    fun `reads empty file`() {
        val sourceFile = workDir.file("app.scripto")
        val textFile = workDir.file("file.txt")

        textFile.writeText("")

        sourceFile.writeText(
            """
val t = readText("${textFile.absolutePath}")
println(t)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("\n")
        }
    }

    @Test
    fun `compiles nested calls to readText function as println argument`() {
        val sourceFile = workDir.file("app.scripto")
        val textFile = workDir.file("file.txt")
        textFile.writeText("string")
        val pathFile = workDir.file("path.txt")
        pathFile.writeText(textFile.absolutePath)

        sourceFile.writeText(
            """
println(readText(readText("${pathFile.absolutePath}")))
        """
        )

        compileAndRun(sourceFile) {
            statements(1)
            output("string\n")
        }
    }

    @Test
    fun `compiles nested calls to readText function as variable value`() {
        val sourceFile = workDir.file("app.scripto")
        val textFile = workDir.file("file.txt")
        textFile.writeText("string")
        val pathFile = workDir.file("path.txt")
        pathFile.writeText(textFile.absolutePath)

        sourceFile.writeText(
            """
val t = readText(readText("${pathFile.absolutePath}"))
println(t)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("string\n")
        }
    }

}