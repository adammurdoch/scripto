package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractScriptIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles empty script`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText("")

        compileAndRun(sourceFile) {
            statements(0)
            output("")
        }
    }

    @Test
    fun `compiles multiple calls to stdlib functions`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println()
println("string")
"""
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("\nstring\n")
        }
    }

    @Test
    fun `compiles unused variable declaration`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = "ignore"
"""
        )

        compileAndRun(sourceFile) {
            statements(1)
            output("")
        }
    }
}