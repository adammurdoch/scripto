package net.rubygrapefruit.scripto.test

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.ir.Program
import kotlin.test.assertEquals

abstract class AppFixture(val workDir: Directory) {
    abstract fun generateAndRun(program: Program): String

    open fun generateAndFail(program: Program) {
        throw IllegalStateException()
    }

    protected fun run(commandLine: List<String>): String {
        return section("Run ${commandLine.joinToString(" ")}") {
            val process = ProcessBuilder(commandLine).redirectErrorStream(true).start()
            process.outputStream.close()
            val exitCode = process.waitFor()
            val output = process.inputReader().readText()
            assertEquals(0, exitCode, "script failed: $output")

            println(output)
            output
        }
    }

    protected inline fun <T> section(title: String, action: () -> T): T {
        println(title)
        println("----")
        val result = action()
        println("----")
        return result
    }

    open fun supports(feature: Feature): Boolean {
        return true
    }
}