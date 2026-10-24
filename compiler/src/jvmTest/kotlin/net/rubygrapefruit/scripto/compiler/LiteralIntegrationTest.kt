package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.scripto.ir.BooleanLiteral
import net.rubygrapefruit.scripto.ir.UnitFunctionCall
import net.rubygrapefruit.scripto.ir.IntLiteral
import net.rubygrapefruit.scripto.ir.StringLiteral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LiteralIntegrationTest : AbstractCompilerIntegrationTest() {
    val sourceFile = workDir.file("app.scripto")

    @Test
    fun `parses string literals`() {
        val candidates = listOf("", "abc")

        for (candidate in candidates) {
            sourceFile.writeText("""println("$candidate")""")

            val program = compile()
            assertEquals(1, program.operations.size)

            val operation = program.operations.first()
            assertIs<UnitFunctionCall>(operation)

            assertEquals(1, operation.arguments.size)

            val literal = operation.arguments.first()
            assertIs<StringLiteral>(literal)

            assertEquals(candidate, literal.value)
        }
    }

    @Test
    fun `parses int literals`() {
        val candidates = listOf(
            "0" to 0,
            "1" to 1,
            "12345" to 12345,
            Int.MAX_VALUE.toString() to Int.MAX_VALUE,
            "-1" to -1,
            Int.MIN_VALUE.toString() to Int.MIN_VALUE,
        )

        for (candidate in candidates) {
            sourceFile.writeText("""println(${candidate.first})""")

            val program = compile()
            assertEquals(1, program.operations.size)

            val operation = program.operations.first()
            assertIs<UnitFunctionCall>(operation)

            assertEquals(1, operation.arguments.size)

            val literal = operation.arguments.first()
            assertIs<IntLiteral>(literal)

            assertEquals(candidate.second, literal.value)
        }
    }

    @Test
    fun `fails when int literal is badly formed`() {
        val candidates = listOf(
            "012" to """")", "+", ",", ".", "==", space, tab""",
            "002" to """")", "+", ",", ".", "==", space, tab""",
            "--12" to """"1".."9""""
        )

        for (candidate in candidates) {
            sourceFile.writeText(
                """
println(${candidate.first})
        """.trimIndent()
            )

            compileFails(
                """
                1 | println(${candidate.first})
                             ^
                Expected ${candidate.second}
            """.trimIndent()
            )
        }
    }

    @Test
    fun `fails when int literal is too large`() {
        sourceFile.writeText("println(${Int.MAX_VALUE.toLong() + 1})")

        compileFails("""1 | println(2147483648)
            ^
Expected ")", "-", "0", "[", "false", "true", '"', an identifier, an integer, space, tab""")
    }

    @Test
    fun `parses boolean literals`() {
        val candidates = listOf(
            "true" to true,
            "false" to false
        )

        for (candidate in candidates) {
            sourceFile.writeText("""println(${candidate.first})""")

            val program = compile()
            assertEquals(1, program.operations.size)

            val operation = program.operations.first()
            assertIs<UnitFunctionCall>(operation)

            assertEquals(1, operation.arguments.size)

            val literal = operation.arguments.first()
            assertIs<BooleanLiteral>(literal)

            assertEquals(candidate.second, literal.value)
        }
    }

    fun compile() = compile(sourceFile)

    fun compileFails(message: String) = compileFails(sourceFile, message)
}