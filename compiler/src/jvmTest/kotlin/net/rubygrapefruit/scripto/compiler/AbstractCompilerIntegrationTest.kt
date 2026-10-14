package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.file.toDir
import org.junit.Rule
import org.junit.rules.TemporaryFolder

abstract class AbstractCompilerIntegrationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    val workDir by lazy { temporaryFolder.newFolder().toDir() }

}