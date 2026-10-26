package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.writeTo
import net.rubygrapefruit.scripto.ir.Program

class NodeGenerator {
    fun generate(program: Program, output: RegularFile) {
        writeTo(output, "//") {
        }
    }
}