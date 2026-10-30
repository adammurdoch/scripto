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
        val javaHome = System.getProperty("java.home")
        for (file in generatedClass.classFiles) {
            run(listOf("$javaHome/bin/javap", "-v", "-p", file.absolutePath))
        }
        return running("$javaHome/bin/java", "-classpath", workDir.absolutePath, generatedClass.name)
    }
}