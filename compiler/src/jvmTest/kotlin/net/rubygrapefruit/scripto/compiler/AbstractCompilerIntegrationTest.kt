package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.file.toDir
import net.rubygrapefruit.scripto.ir.Program
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs

abstract class AbstractCompilerIntegrationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    val workDir by lazy { temporaryFolder.newFolder().toDir() }

    init {
        temporaryFolder.create()
    }

    protected fun compile(sourceFile: RegularFile): Program {
        return Compiler().compile(sourceFile).get()
    }

    protected fun compileFails(sourceFile: RegularFile, message: String) {
        val compiler = Compiler()
        val result = compiler.compile(sourceFile)
        assertIs<Compiler.Result.Failure>(result)
        assertEquals(listOf(message), result.error.map { it.message })
        val e = assertFails {
            result.get()
        }
        assertEquals("Compile failed with 1 error", e.message)
    }

    protected fun compileFails(sourceFile: RegularFile, messages: List<String>) {
        val compiler = Compiler()
        val result = compiler.compile(sourceFile)
        assertIs<Compiler.Result.Failure>(result)
        assertEquals(messages, result.error.map { it.message })
        val e = assertFails {
            result.get()
        }
        assertEquals("Compile failed with ${messages.size} errors", e.message)
    }
}