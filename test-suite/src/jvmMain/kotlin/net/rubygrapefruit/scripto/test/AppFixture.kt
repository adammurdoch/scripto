package net.rubygrapefruit.scripto.test

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.ir.Program
import kotlin.test.assertEquals

abstract class AppFixture(val workDir: Directory) {

    fun generateAndRun(program: Program, expectedOutput: String) {
        val output = generateAndRun(program)
        assertEquals(expectedOutput, output)
    }

    protected abstract fun generateAndRun(program: Program): String

    protected fun run(commandLine: List<String>): String {
        val process = ProcessBuilder(commandLine).redirectErrorStream(true).start()
        process.outputStream.close()
        val exitCode = process.waitFor()
        val output = process.inputReader().readText()
        assertEquals(0, exitCode, "script failed: $output")

        println("$commandLine produced '$output'")

        return output
    }
}