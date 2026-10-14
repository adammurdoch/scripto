package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.test.AppFixture

class BashFixture(workDir: Directory): AppFixture(workDir) {
    override fun generateAndRun(program: Program): String {
        val generator = BashGenerator()
        val outputScript = workDir.file("app.bash")
        generator.generate(program, outputScript)

        println("Generated source:")
        println(outputScript.readText())
        return run(listOf(outputScript.absolutePath))
    }
}