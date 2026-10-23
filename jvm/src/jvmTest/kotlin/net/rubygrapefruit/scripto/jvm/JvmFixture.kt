package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.test.AppFixture

class JvmFixture(workDir: Directory) : AppFixture(workDir) {
    override fun generateAndRun(program: Program): String {
        val generator = JvmGenerator()
        val generatedClass = generator.generate(program, workDir)
        for (file in generatedClass.classFiles) {
            run(listOf("javap", "-v", "-p", file.absolutePath))
        }
        return run(listOf("java", "-classpath", workDir.absolutePath, generatedClass.name))
    }
}