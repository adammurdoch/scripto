package net.rubygrapefruit.scripto.compiler

import kotlin.test.Test
import kotlin.test.assertEquals

class WhitespaceIntegrationTest : AbstractCompilerIntegrationTest() {
    val sourceFile = workDir.file("app.scripto")

    @Test
    fun `parses script with whitespace only`() {
        val candidates = listOf(
            "",
            "\n",
            "   \n   \n   \n\n"
        )
        for (candidate in candidates) {
            sourceFile.writeText(candidate)

            val program = compile()
            assertEquals(0, program.operations.size)
        }
    }

    @Test
    fun `parses script with line comments only`() {
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

            val program = compile()
            assertEquals(0, program.operations.size)
        }
    }

    @Test
    fun `parses script with blank lines around statements`() {
        val candidates = listOf(
            "\n",
            "   \n   \n   \n\n"
        )
        for (candidate in candidates) {
            sourceFile.writeText("${candidate}println()${candidate}println()$candidate")

            val program = compile()
            assertEquals(2, program.operations.size)
        }
    }

    @Test
    fun `parses script with line comment on same line as statement`() {
        val candidates = listOf(
            "println() // comment",
            "val a = 12 // comment",
            "val a = 12// println()",
            "println() // comment\n",
        )

        for (candidate in candidates) {
            sourceFile.writeText(candidate)

            val program = compile()
            assertEquals(1, program.operations.size)
        }
    }

    @Test
    fun `ignores whitespace around function operator`() {
        val candidates = listOf(
            "println(123.toString())",
            "println(123 . toString())",
            "println(123+123)",
            "println(123 + 123)"
        )

        for (candidate in candidates) {
            sourceFile.writeText(candidate)

            val program = compile()
            assertEquals(1, program.operations.size)
        }
    }

    @Test
    fun `parses script with no trailing end-of-line`() {
        sourceFile.writeText("println()")

        val program = compile()
        assertEquals(1, program.operations.size)
    }

    fun compile() = compile(sourceFile)
}