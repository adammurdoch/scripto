package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.writeTo
import net.rubygrapefruit.scripto.ir.Program

class NodeGenerator {
    fun generate(program: Program, output: Directory): RegularFile {
        val outputFile = output.file("${program.snakeCaseName}.js")
        writeTo(outputFile, "//") {
        }
        return outputFile
    }
}