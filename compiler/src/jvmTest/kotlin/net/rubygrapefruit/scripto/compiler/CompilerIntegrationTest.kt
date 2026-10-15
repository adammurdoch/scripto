package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.scripto.ir.VariableDeclaration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CompilerIntegrationTest : AbstractCompilerIntegrationTest() {
    val sourceFile = workDir.file("app.scripto")

    @Test
    fun `parses variable names`() {

        val candidates = listOf("a", "abc", "a1", "theVar", "the_var")

        for (candidate in candidates) {
            sourceFile.writeText(
                """
val $candidate = "string"
            """
            )

            val program = compile()
            assertEquals(1, program.operations.size)

            val operation = program.operations.first()
            assertIs<VariableDeclaration>(operation)

            assertEquals(candidate, operation.name)
        }
    }

    @Test
    fun `fails with syntax error`() {
        sourceFile.writeText(
            """
text!
        """.trimIndent()
        )

        compileFails("""1 | text!
        ^
Expected "(", "A".."Z", "_", "a".."z", a digit""")
    }

    @Test
    fun `fails when unknown variable used as function argument`() {
        sourceFile.writeText(
            """
println(unknown)
        """.trimIndent()
        )

        compileFails("8: Unknown variable 'unknown'")
    }

    @Test
    fun `fails when unknown variable used as variable initializer`() {
        sourceFile.writeText(
            """
val a = b
        """.trimIndent()
        )

        compileFails("8: Unknown variable 'b'")
    }

    @Test
    fun `fails when duplicate variable declared`() {
        sourceFile.writeText(
            """
val a = 1
val a = 2
        """.trimIndent()
        )

        compileFails("Variable 'a' declared multiple times")
    }

    @Test
    fun `fails when unknown function called`() {
        sourceFile.writeText(
            """
unknown()
        """.trimIndent()
        )

        compileFails("Unknown function unknown()")
    }

    @Test
    fun `fails when incorrect number of arguments provided to function`() {
        sourceFile.writeText(
            """
println("a", "b")
        """.trimIndent()
        )

        compileFails("Incorrect number of parameters for function println()")
    }

    @Test
    fun `fails when unknown function called on receiver`() {
        sourceFile.writeText(
            """
val s = "text".unknown()
        """.trimIndent()
        )

        compileFails("Unknown function String.unknown()")
    }

    @Test
    fun `fails when incorrect number of arguments provided to receiver function`() {
        sourceFile.writeText(
            """
val s = "text".toString(1, 2)
        """.trimIndent()
        )

        compileFails("Incorrect number of parameters for function String.toString()")
    }

    @Test
    fun `fails when left argument of int addition operator has incorrect type`() {
        sourceFile.writeText(
            """
println(false + 2)
        """.trimIndent()
        )

        compileFails("Argument is not of type Int")
    }

    @Test
    fun `fails when right argument of int addition operator has incorrect type`() {
        sourceFile.writeText(
            """
println(2 + "string")
        """.trimIndent()
        )

        compileFails("Argument is not of type Int")
    }

    @Test
    fun `reports multiple failures`() {
        sourceFile.writeText(
            """
unknown1()
println("a", "b")
        """.trimIndent()
        )

        compileFails(listOf("Unknown function unknown1()", "Incorrect number of parameters for function println()"))
    }

    fun compile() = compile(sourceFile)

    fun compileFails(message: String) = compileFails(sourceFile, message)

    fun compileFails(messages: List<String>) = compileFails(sourceFile, messages)
}