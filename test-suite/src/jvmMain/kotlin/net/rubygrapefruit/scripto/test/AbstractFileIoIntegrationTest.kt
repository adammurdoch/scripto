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
val t = File("${textFile.absolutePath}").readText()
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
val t = File("${textFile.absolutePath}").readText()
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
val t = File("${textFile.absolutePath}").readText()
println(t)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("\n")
        }
    }

    @Test
    fun `compiles chained calls to readText function as function argument`() {
        val sourceFile = workDir.file("app.scripto")
        val textFile = workDir.file("file.txt")
        textFile.writeText("string")
        val pathFile = workDir.file("path.txt")
        pathFile.writeText(textFile.absolutePath)

        sourceFile.writeText(
            """
println(File(File("${pathFile.absolutePath}").readText()).readText())
        """
        )

        compileAndRun(sourceFile) {
            statements(1)
            output("string\n")
        }
    }

    @Test
    fun `compiles chained calls to readText function as variable value`() {
        val sourceFile = workDir.file("app.scripto")
        val textFile = workDir.file("file.txt")
        textFile.writeText("string")
        val pathFile = workDir.file("path.txt")
        pathFile.writeText(textFile.absolutePath)

        sourceFile.writeText(
            """
val t = File(File("${pathFile.absolutePath}").readText()).readText()
println(t)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("string\n")
        }
    }

    @Test
    fun `compiles concatenated calls to readText function as function argument`() {
        val sourceFile = workDir.file("app.scripto")
        val file1 = workDir.file("file1.txt")
        file1.writeText("string")
        val file2 = workDir.file("file2.txt")
        file2.writeText("!")

        sourceFile.writeText(
            """
println(File("${file1.absolutePath}").readText() + File("${file2.absolutePath}").readText())
        """
        )

        compileAndRun(sourceFile) {
            statements(1)
            output("string!\n")
        }
    }

}