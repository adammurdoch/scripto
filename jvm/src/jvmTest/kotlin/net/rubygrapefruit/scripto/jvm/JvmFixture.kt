package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.test.AppFixture

class JvmFixture(workDir: Directory) : AppFixture(workDir) {
    override fun generateAndRun(program: Program): String {
        val generator = JvmGenerator()
        val classFile = workDir.file("App.class")
        val generatedClass = generator.generate(program, classFile)
        run(listOf("javap", "-v", "-p", classFile.absolutePath))
        return run(listOf("java", "-classpath", workDir.absolutePath, generatedClass.name))
    }
}