package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.c.compile.CCompiler
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.test.AppFixture

class CFixture(workDir: Directory) : AppFixture(workDir) {
    override fun generate(program: Program): App {
        val generator = CGenerator()

        val cSource = generator.generate(program, workDir)

        section("Generated source") {
            println(cSource.readText())
        }

        val exe = workDir.file("app")
        CCompiler().compile(cSource, exe)
        return running(exe.absolutePath)
    }
}