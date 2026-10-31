package net.rubygrapefruit.scripto.test

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.file.toDir
import net.rubygrapefruit.machine.info.Machine
import net.rubygrapefruit.scripto.compiler.Compiler
import net.rubygrapefruit.scripto.ir.Program
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

abstract class AbstractIntegrationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    val workDir by lazy { temporaryFolder.newFolder().toDir() }

    init {
        temporaryFolder.create()
    }

    abstract fun fixture(): AppFixture

    protected fun compileAndRun(sourceFile: RegularFile, features: List<Feature> = emptyList(), builder: InspectionFixture.() -> Unit) {
        compile(sourceFile, features) {
            execute {
                DefaultInspectionFixture(this@compile, this).builder()
            }
        }
    }

    protected fun compile(sourceFile: RegularFile, features: List<Feature> = emptyList(), config: CompilationResultFixture.() -> Unit) {
        val fixture = fixture()
        val compiler = Compiler()
        val program = compiler.compile(sourceFile, fixture.targetRuntime).get()
        if (features.any { !fixture.supports(it) }) {
            fixture.generationFails(program)
        } else {
            val app = fixture.generate(program)
            DefaultCompilationResultFixture(program, app, Machine.thisMachine.isWindows && fixture.windowsLineEndings).config()
        }
    }

    interface InspectionFixture {
        fun statements(expectedCount: Int)
        fun output(expected: String)
    }

    private class DefaultInspectionFixture(val compile: CompilationResultFixture, val execute: ExecutionResultFixture) : InspectionFixture {
        override fun statements(expectedCount: Int) {
            compile.statements(expectedCount)
        }

        override fun output(expected: String) {
            execute.output(expected)
        }
    }

    interface CompilationResultFixture {
        fun statements(expectedCount: Int)
        fun execute(vararg args: String, config: ExecutionResultFixture.() -> Unit)

        fun output(expected: String) {
            execute {
                output(expected)
            }
        }
    }

    private class DefaultCompilationResultFixture(val program: Program, val app: AppFixture.App, val windowsLineEndings: Boolean) : CompilationResultFixture {
        override fun statements(expectedCount: Int) {
            assertEquals(expectedCount, program.operations.size)
        }

        override fun execute(vararg args: String, config: ExecutionResultFixture.() -> Unit) {
            val output = app.execute(args.toList())
            DefaultExecutionResultFixture(output, windowsLineEndings).config()
        }
    }

    interface ExecutionResultFixture {
        fun output(expected: String)
    }

    private class DefaultExecutionResultFixture(val output: String, val windowsLineEndings: Boolean) : ExecutionResultFixture {
        override fun output(expected: String) {
            val normalizedExpected = if (windowsLineEndings) {
                expected.replace("\n", "\r\n")
            } else {
                expected
            }
            assertEquals(normalizedExpected, output)
        }
    }
}