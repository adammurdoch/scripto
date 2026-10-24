package net.rubygrapefruit.scripto.c.compile

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.process.Process

class CCompiler {
    fun compile(source: RegularFile, output: RegularFile) {
        val process = Process.command("clang", "-o", output.absolutePath, source.absolutePath).collectExitCode().start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("Could not compile source file $source")
        }
    }
}