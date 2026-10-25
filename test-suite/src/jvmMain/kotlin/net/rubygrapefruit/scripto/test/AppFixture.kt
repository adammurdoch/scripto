package net.rubygrapefruit.scripto.test

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.ir.TargetRuntime
import kotlin.test.assertEquals

abstract class AppFixture(val workDir: Directory) {
    open val targetRuntime: TargetRuntime
        get() = TargetRuntime.NoOp

    abstract fun generate(program: Program): App

    open fun generationFails(program: Program) {
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

    protected fun running(commandLine: List<String>): App {
        return object : App {
            override fun execute(args: List<String>): String {
                return run(commandLine + args)
            }
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

    interface App {
        fun execute(args: List<String>): String
    }
}