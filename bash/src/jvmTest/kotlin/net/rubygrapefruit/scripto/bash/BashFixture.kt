package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.file.missing
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.test.AppFixture
import net.rubygrapefruit.scripto.test.Feature
import kotlin.test.assertFails
import kotlin.test.assertTrue

class BashFixture(workDir: Directory) : AppFixture(workDir) {
    override fun generateAndRun(program: Program): String {
        val generator = BashGenerator()
        val outputScript = workDir.file("app.bash")
        generator.generate(program, outputScript)

        section("Generated source") {
            println(outputScript.readText())
        }

        return run(listOf(outputScript.absolutePath))
    }

    override fun generateAndFail(program: Program) {
        val generator = BashGenerator()
        val outputScript = workDir.file("app.bash")

        val e = assertFails { generator.generate(program, outputScript) }
        println("Generation failed with: ${e.message}")
        assertTrue(e.message!!.endsWith(" is not supported for Bash"))

        assertTrue(outputScript.metadata().missing)
    }

    override fun supports(feature: Feature): Boolean {
        return feature != Feature.ListOfList
    }
}