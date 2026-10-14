package net.rubygrapefruit.scripto.cli

import net.rubygrapefruit.cli.app.CliAction
import net.rubygrapefruit.cli.app.CliApp
import net.rubygrapefruit.scripto.jvm.JvmGenerator
import net.rubygrapefruit.scripto.bash.BashGenerator
import net.rubygrapefruit.scripto.c.CGenerator
import net.rubygrapefruit.scripto.c.compile.CCompiler
import net.rubygrapefruit.scripto.compiler.Compiler
import net.rubygrapefruit.scripto.powershell.PowershellGenerator
import java.lang.reflect.InvocationTargetException

class ScriptoApp : CliApp("scripto") {
    val action by action {
        action(CompileAction(), "compile")
        action(RunAction(), "run")
    }

    override fun run() {
        action.run()
    }
}

class CompileAction : CliAction() {
    val outputDir by dir(mustExist = false).option("out").required()
    val sourceFile by file(mustExist = true).option("source").required()

    override fun run() {
        val compiler = Compiler()
        val program = compiler.compile(sourceFile)

        val bashGenerator = BashGenerator()
        bashGenerator.generate(program, outputDir.file("app.bash"))

        val powershellGenerator = PowershellGenerator()
        powershellGenerator.generate(program, outputDir.file("app.ps"))

        val cGenerator = CGenerator()
        val cSourceFile = outputDir.file("app.c")
        cGenerator.generate(program, cSourceFile)
        val cCompiler = CCompiler()
        cCompiler.compile(cSourceFile, outputDir.file("app"))

        val jvmGenerator = JvmGenerator()
        val classFile = outputDir.file("App.class")
        jvmGenerator.generate(program, classFile)
    }
}

class RunAction : CliAction() {
    val sourceFile by file(mustExist = true).option("source").required()

    override fun run() {
        val compiler = Compiler()
        val program = compiler.compile(sourceFile)

        val jvmGenerator = JvmGenerator()
        val generatedClass = jvmGenerator.generate(program)

        val classLoader = CustomClassLoader(generatedClass.className, generatedClass.byteCode, javaClass.classLoader)
        val loadedClass = classLoader.loadClass(generatedClass.className)
        val method = loadedClass.getDeclaredMethod("main", String::class.java.arrayType())
        try {
            method.invoke(null, emptyArray<String>())
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    class CustomClassLoader(val className: String, val bytecode: ByteArray, parent: ClassLoader?) : ClassLoader(parent) {
        override fun findClass(name: String): Class<*>? {
            return if (name == className) {
                defineClass(className, bytecode, 0, bytecode.size)
            } else {
                super.findClass(name)
            }
        }
    }
}

fun main(args: Array<String>) = ScriptoApp().run(args)
