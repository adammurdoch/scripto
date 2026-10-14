package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.ir.*
import kotlin.test.*

class CompilerIntegrationTest : AbstractCompilerIntegrationTest() {

    @Test
    fun `parses string literals`() {
        val sourceFile = workDir.file("app.scripto")

        val candidates = listOf("", "abc")

        for (candidate in candidates) {
            sourceFile.writeText("""println("$candidate")""")

            val program = Compiler().compile(sourceFile)
            assertEquals(1, program.operations.size)

            val operation = program.operations.first()
            assertIs<FunctionCall>(operation)

            assertEquals(1, operation.arguments.size)

            val literal = operation.arguments.first()
            assertIs<StringLiteral>(literal)

            assertEquals(candidate, literal.value)
        }
    }

    @Test
    fun `parses int literals`() {
        val sourceFile = workDir.file("app.scripto")

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

            val program = Compiler().compile(sourceFile)
            assertEquals(1, program.operations.size)

            val operation = program.operations.first()
            assertIs<FunctionCall>(operation)

            assertEquals(1, operation.arguments.size)

            val literal = operation.arguments.first()
            assertIs<IntLiteral>(literal)

            assertEquals(candidate.second, literal.value)
        }
    }

    @Test
    fun `fails when int literal is badly formed`() {
        val sourceFile = workDir.file("app.scripto")

        val candidates = listOf(
            "012",
            "002",
            "--12"
        )

        for (candidate in candidates) {
            sourceFile.writeText(
                """
println($candidate)
        """.trimIndent()
            )

            compileFails(sourceFile, """
                1 | println($candidate)
                             ^
                Expected
            """.trimIndent())
        }
    }

    @Test
    fun `fails when int literal is too large`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText("println(${Int.MAX_VALUE.toLong() + 1})")

        compileFails(sourceFile, "For input string: \"2147483648\"")
    }

    @Test
    fun `parses boolean literals`() {
        val sourceFile = workDir.file("app.scripto")

        val candidates = listOf(
            "true" to true,
            "false" to false
        )

        for (candidate in candidates) {
            sourceFile.writeText("""println(${candidate.first})""")

            val program = Compiler().compile(sourceFile)
            assertEquals(1, program.operations.size)

            val operation = program.operations.first()
            assertIs<FunctionCall>(operation)

            assertEquals(1, operation.arguments.size)

            val literal = operation.arguments.first()
            assertIs<BooleanLiteral>(literal)

            assertEquals(candidate.second, literal.value)
        }
    }

    @Test
    fun `parses variable names`() {
        val sourceFile = workDir.file("app.scripto")

        val candidates = listOf("a", "abc", "a1", "theVar", "the_var")

        for (candidate in candidates) {
            sourceFile.writeText(
                """
val $candidate = "string"
            """
            )

            val program = Compiler().compile(sourceFile)
            assertEquals(1, program.operations.size)

            val operation = program.operations.first()
            assertIs<VariableDeclaration>(operation)

            assertEquals(candidate, operation.name)
        }
    }

    @Test
    fun `fails when unknown variable used as function argument`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(unknown)
        """.trimIndent()
        )

        compileFails(sourceFile, "Compile failed: Unknown variable 'unknown'")
    }

    @Test
    fun `fails when unknown variable used as variable initializer`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = b
        """.trimIndent()
        )

        compileFails(sourceFile, "Compile failed: Unknown variable 'b'")
    }

    @Test
    fun `fails when duplicate variable declared`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = 1
val a = 2
        """.trimIndent()
        )

        compileFails(sourceFile, "Compile failed: Variable 'a' declared multiple times")
    }

    @Test
    fun `fails when unknown function called`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
unknown()
        """.trimIndent()
        )

        compileFails(sourceFile, "Compile failed: Unknown function 'unknown'")
    }

    @Test
    fun `fails when incorrect number of arguments provided to function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println("a", "b")
        """.trimIndent()
        )

        compileFails(sourceFile, "Compile failed: Incorrect number of parameters for function 'println'")
    }

    @Test
    fun `fails when left argument of int addition operator has incorrect type`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(false + 2)
        """.trimIndent()
        )

        compileFails(sourceFile, "Compile failed: Argument is not of type Int")
    }

    @Test
    fun `fails when right argument of int addition operator has incorrect type`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(2 + "string")
        """.trimIndent()
        )

        compileFails(sourceFile, "Compile failed: Argument is not of type Int")
    }

    @Test
    fun `reports multiple failures`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
unknown1()
println("a", "b")
        """.trimIndent()
        )

        compileFails(sourceFile, "Compile failed: Unknown function 'unknown1', Incorrect number of parameters for function 'println'")
    }

    private fun compileFails(sourceFile: RegularFile, message: String) {
        val compiler = Compiler()
        try {
            compiler.compile(sourceFile)
        } catch (e: Exception) {
            val actual = e.message
            assertTrue(actual != null && actual.startsWith(message), "Unexpected error message: $actual")
            return
        }
        fail()
    }
}