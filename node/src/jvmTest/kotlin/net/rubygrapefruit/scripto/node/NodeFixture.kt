package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.machine.info.Machine
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.test.AppFixture

class NodeFixture(workDir: Directory) : AppFixture(workDir) {
    override fun generate(program: Program): App {
        val generator = NodeGenerator()
        val outputScript = generator.generate(program, workDir)

        section("Generated source") {
            println(outputScript.readText())
        }

        val nodeExe = if (Machine.thisMachine.isWindows) {
            "build/node-js/node.exe"
        } else {
            "build/node-js/bin/node"
        }

        return running(nodeExe, outputScript.absolutePath)
    }
}