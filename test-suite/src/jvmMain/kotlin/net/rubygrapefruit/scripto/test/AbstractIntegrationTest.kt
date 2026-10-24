package net.rubygrapefruit.scripto.test

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.file.toDir
import net.rubygrapefruit.scripto.compiler.Compiler
import net.rubygrapefruit.scripto.ir.Program
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

abstract class AbstractIntegrationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    val workDir by lazy { temporaryFolder.newFolder().toDir() }

    abstract fun fixture(): AppFixture

    protected fun compileAndRun(sourceFile: RegularFile, vararg features: Feature, builder: InspectionFixture.() -> Unit) {
        val spec = DefaultInspectionFixture()
        spec.builder()
        val compiler = Compiler()
        val program = compiler.compile(sourceFile).get()
        spec.inspect(program)
        val fixture = fixture()
        if (features.any { !fixture.supports(it) }) {
            fixture.generateAndFail(program)
        } else {
            val output = fixture.generateAndRun(program)
            assertEquals(spec.expectedOutput, output)
        }
    }

    interface InspectionFixture {
        fun statements(count: Int)
        fun output(text: String)
    }

    private class DefaultInspectionFixture : InspectionFixture {
        var expectedStatements = -1
        var expectedOutput = ""

        override fun statements(count: Int) {
            expectedStatements = count
        }

        override fun output(text: String) {
            expectedOutput = text
        }

        fun inspect(program: Program) {
            if (expectedStatements > -1) {
                assertEquals(expectedStatements, program.operations.size)
            }
        }
    }
}