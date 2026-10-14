package net.rubygrapefruit.scripto.compiler

import kotlin.test.Test
import kotlin.test.assertEquals

class WhitespaceIntegrationTest : AbstractCompilerIntegrationTest() {
    @Test
    fun `parses script with whitespace only`() {
        val sourceFile = workDir.file("app.scripto")

        val candidates = listOf(
            "",
            "\n",
            "   \n   \n   \n\n"
        )
        for (candidate in candidates) {
            sourceFile.writeText(candidate)

            val program = Compiler().compile(sourceFile)
            assertEquals(0, program.operations.size)
        }
    }

    @Test
    fun `parses script with line comments only`() {
        val sourceFile = workDir.file("app.scripto")

        val candidates = listOf(
            "//",
            "// no trailing end of line",
            "  // leading whitespace",
            "// nested // comment",
            """
                // comment
            """.trimIndent(),
            """
                // comments
                  // with mixed
                // indentation
            """.trimIndent(),
            """
                // comment
                
                // another comment""".trimIndent(),
        )
        for (candidate in candidates) {
            sourceFile.writeText(candidate)

            val program = Compiler().compile(sourceFile)
            assertEquals(0, program.operations.size)
        }
    }

    @Test
    fun `parses script with blank lines around statements`() {
        val sourceFile = workDir.file("app.scripto")

        val candidates = listOf(
            "\n",
            "   \n   \n   \n\n"
        )
        for (candidate in candidates) {
            sourceFile.writeText("${candidate}println()${candidate}println()$candidate")

            val program = Compiler().compile(sourceFile)
            assertEquals(2, program.operations.size)
        }
    }

    @Test
    fun `parses script with line comment on same line as statement`() {
        val sourceFile = workDir.file("app.scripto")

        val candidates = listOf(
            "println() // comment",
            "val a = 12 // comment",
            "val a = 12// println()",
            "println() // comment\n",
        )

        for (candidate in candidates) {
            sourceFile.writeText(candidate)

            val program = Compiler().compile(sourceFile)
            assertEquals(1, program.operations.size)
        }
    }

    @Test
    fun `parses script with no trailing end-of-line`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText("println()")

        val program = Compiler().compile(sourceFile)
        assertEquals(1, program.operations.size)
    }
}