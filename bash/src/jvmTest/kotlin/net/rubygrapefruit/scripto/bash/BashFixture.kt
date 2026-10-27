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

    override fun generate(program: Program): App {
        val generator = BashGenerator()
        val outputScript = generator.generate(program, workDir)

        section("Generated source") {
            println(outputScript.readText())
        }

        return running(outputScript.absolutePath)
    }

    override fun generationFails(program: Program) {
        val outputScript = targetRuntime.generate(program, workDir)
        assertTrue(outputScript.metadata().missing)
    }

    override fun supports(feature: Feature): Boolean {
        return feature != Feature.ListOfList
    }
}