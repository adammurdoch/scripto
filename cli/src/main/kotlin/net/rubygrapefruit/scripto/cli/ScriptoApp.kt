package net.rubygrapefruit.scripto.cli

import net.rubygrapefruit.cli.app.CliAction
import net.rubygrapefruit.cli.app.CliApp
import net.rubygrapefruit.script.interpreter.Interpreter
import net.rubygrapefruit.script.interpreter.StdOut
import net.rubygrapefruit.scripto.bash.BashGenerator
import net.rubygrapefruit.scripto.c.CGenerator
import net.rubygrapefruit.scripto.c.compile.CCompiler
import net.rubygrapefruit.scripto.compiler.Compiler
import net.rubygrapefruit.scripto.jvm.JvmGenerator
import net.rubygrapefruit.scripto.powershell.PowershellGenerator

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
        val program = compiler.compile(sourceFile).get()

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
        val program = compiler.compile(sourceFile).get()

        val interpreter = Interpreter()
        val stdout = object : StdOut {
            override fun println(text: String) {
                kotlin.io.println(text)
            }
        }
        interpreter.run(program, stdout)
    }
}

fun main(args: Array<String>) = ScriptoApp().run(args)
