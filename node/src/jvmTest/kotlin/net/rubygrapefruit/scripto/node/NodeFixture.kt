package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.test.AppFixture
import java.io.File

class NodeFixture(workDir: Directory) : AppFixture(workDir) {
    override fun generate(program: Program): App {
        val generator = NodeGenerator()
        val outputScript = generator.generate(program, workDir)

        section("Generated source") {
            println(outputScript.readText())
        }

        val nodeInstall = File("build/node-js").absoluteFile
        println("NODE")
        println("install: $nodeInstall (${nodeInstall.isDirectory})")

        nodeInstall.listFiles()?.forEach {
            println(it)
        }

        return running("build/node-js/bin/node", outputScript.absolutePath)
    }
}