package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractMainArgsIntegrationTest: AbstractIntegrationTest() {
    val sourceFile = workDir.file("app.scripto")

    @Test
    fun `can use implicit args variable`() {
        sourceFile.writeText("""
println(args)            
        """.trimIndent())

        compileAndRun(sourceFile) {
            output("[]\n")
        }
        compileAndRun(sourceFile, args = listOf("1", "2")) {
            output("[1, 2]\n")
        }
        compileAndRun(sourceFile, args = listOf("--flag", "--no-flag")) {
            output("[--flag, --no-flag]\n")
        }
    }
}