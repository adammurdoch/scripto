package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.file.missing
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.ir.TargetRuntime
import net.rubygrapefruit.scripto.test.AppFixture
import net.rubygrapefruit.scripto.test.Feature
import kotlin.test.assertTrue

class BashFixture(workDir: Directory) : AppFixture(workDir) {
    override val targetRuntime: TargetRuntime
        field = BashGenerator()

    override fun generateAndRun(program: Program, args: List<String>): String {
        val generator = BashGenerator()
        val outputScript = workDir.file("app.bash")
        generator.generate(program, outputScript)

        section("Generated source") {
            println(outputScript.readText())
        }

        return run(listOf(outputScript.absolutePath) + args)
    }

    override fun generationFails(program: Program) {
        val outputScript = workDir.file("app.bash")

        targetRuntime.generate(program, outputScript)
        assertTrue(outputScript.metadata().missing)
    }

    override fun supports(feature: Feature): Boolean {
        return feature != Feature.ListOfList
    }
}