package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.test.AppFixture

class NodeFixture(workDir: Directory) : AppFixture(workDir) {
    override fun generate(program: Program): App {
        val output = workDir.file("app.js")
        val generator = NodeGenerator()
        generator.generate(program, output)

        return running("build/node-js/bin/node", output.absolutePath)
    }
}