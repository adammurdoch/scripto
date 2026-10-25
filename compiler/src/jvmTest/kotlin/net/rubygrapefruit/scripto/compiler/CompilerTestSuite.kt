package net.rubygrapefruit.scripto.compiler

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestFixture.Scope
import de.infix.testBalloon.framework.core.TestSuite
import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.file.toDir
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.ir.TargetRuntime
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs

class SourceFileFixture(private val workDir: Directory, val sourceFile: RegularFile) {
    fun compile(sourceFile: RegularFile = this.sourceFile): Program {
        return Compiler().compile(sourceFile, TargetRuntime.NoOp).get()
    }

    fun compileFails(message: String, sourceFile: RegularFile = this.sourceFile) {
        val compiler = Compiler()
        val result = compiler.compile(sourceFile, TargetRuntime.NoOp)
        assertIs<Compiler.Result.Failure>(result)
        assertEquals(listOf(message), result.errors.map { it.message })
        val e = assertFails {
            result.get()
        }
        assertEquals("Compile failed with 1 error", e.message)
    }

    fun compileFails(messages: List<String>, sourceFile: RegularFile = this.sourceFile) {
        val compiler = Compiler()
        val result = compiler.compile(sourceFile, TargetRuntime.NoOp)
        assertIs<Compiler.Result.Failure>(result)
        assertEquals(messages, result.errors.map { it.message })
        val e = assertFails {
            result.get()
        }
        assertEquals("Compile failed with ${messages.size} errors", e.message)
    }

    companion object {
        fun of(): SourceFileFixture {
            val tmpDir = Path("build/test files").absolute()
            tmpDir.createDirectories()
            val workDir = Files.createTempDirectory(tmpDir, "test").toDir()
            val sourceFile = workDir.file("app.scripto")
            return SourceFileFixture(workDir, sourceFile)
        }
    }
}

fun TestSuite.withCompiler(content: Scope<suspend SourceFileFixture.(testExecutionScope: Test.ExecutionScope) -> Unit>.() -> Unit) {
    testFixture {
        SourceFileFixture.of()
    }.asContextForEach {
        content()
    }
}