package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.test.AppFixture

class JvmFixture(workDir: Directory) : AppFixture(workDir) {
    override fun generate(program: Program): App {
        val generator = JvmGenerator()
        val generatedClass = generator.generate(program, workDir)
        println("GENERATED")
        for (file in generatedClass.classFiles) {
            println("  ${file.path.name}")
        }
        for (file in generatedClass.classFiles) {
            run(listOf("javap", "-v", "-p", file.absolutePath))
        }
        return running(listOf("java", "-classpath", workDir.absolutePath, generatedClass.name))
    }
}