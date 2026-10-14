package net.rubygrapefruit.scripto.c.compile

import net.rubygrapefruit.file.RegularFile

class CCompiler {
    fun compile(source: RegularFile, output: RegularFile) {
        val process = ProcessBuilder("clang", "-o", output.absolutePath, source.absolutePath)
            .redirectErrorStream(true)
//            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start()
        val out = process.inputReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("Could not compile source file $source: $out")
        }
    }
}