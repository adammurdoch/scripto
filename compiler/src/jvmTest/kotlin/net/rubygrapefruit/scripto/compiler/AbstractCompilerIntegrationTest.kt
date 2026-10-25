package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.file.toDir
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.ir.TargetRuntime
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs

@OptIn(ExperimentalPathApi::class)
abstract class AbstractCompilerIntegrationTest {
    private var temporaryFolder: Path? = null

    val workDir by lazy {
        val tmpDir = Path("build/test files").absolute()
        tmpDir.createDirectories()
        temporaryFolder = Files.createTempDirectory(tmpDir, "test")
        temporaryFolder!!.toDir()
    }

    @AfterTest
    fun cleanup() {
        temporaryFolder?.deleteRecursively()
    }

    protected fun compile(sourceFile: RegularFile): Program {
        return Compiler().compile(sourceFile, TargetRuntime.NoOp).get()
    }

    protected fun compileFails(sourceFile: RegularFile, message: String) {
        val compiler = Compiler()
        val result = compiler.compile(sourceFile, TargetRuntime.NoOp)
        assertIs<Compiler.Result.Failure>(result)
        assertEquals(listOf(message), result.errors.map { it.message })
        val e = assertFails {
            result.get()
        }
        assertEquals("Compile failed with 1 error", e.message)
    }

    protected fun compileFails(sourceFile: RegularFile, messages: List<String>) {
        val compiler = Compiler()
        val result = compiler.compile(sourceFile, TargetRuntime.NoOp)
        assertIs<Compiler.Result.Failure>(result)
        assertEquals(messages, result.errors.map { it.message })
        val e = assertFails {
            result.get()
        }
        assertEquals("Compile failed with ${messages.size} errors", e.message)
    }
}