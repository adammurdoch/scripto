package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.c.compile.CCompiler
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.test.AppFixture

class CFixture(workDir: Directory) : AppFixture(workDir) {
    override fun generateAndRun(program: Program, args: List<String>): String {
        val generator = CGenerator()

        val cSource = workDir.file("app.c")
        generator.generate(program, cSource)

        section("Generated source") {
            println(cSource.readText())
        }

        val exe = workDir.file("app")
        CCompiler().compile(cSource, exe)
        return run(listOf(exe.absolutePath) + args)
    }
}