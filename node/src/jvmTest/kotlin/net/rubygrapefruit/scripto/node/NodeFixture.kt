package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.test.AppFixture

class NodeFixture(workDir: Directory) : AppFixture(workDir) {
    override fun generate(program: Program): App {
        val generator = NodeGenerator()
        val outputScript = generator.generate(program, workDir)

        return running("build/node-js/bin/node", outputScript.absolutePath)
    }
}