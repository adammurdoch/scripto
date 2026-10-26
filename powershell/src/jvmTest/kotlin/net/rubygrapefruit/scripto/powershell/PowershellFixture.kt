package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.test.AppFixture

class PowershellFixture(workDir: Directory) : AppFixture(workDir) {
    override fun generate(program: Program): App {
        val generator = PowershellGenerator()
        val outputScript = workDir.file("app.ps")
        generator.generate(program, outputScript)

        section("Generated source") {
            println(outputScript.readText())
        }

        return running("build/powershell/pwsh", outputScript.absolutePath)
    }
}