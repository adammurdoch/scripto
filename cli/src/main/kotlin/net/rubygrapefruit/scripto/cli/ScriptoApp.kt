package net.rubygrapefruit.scripto.cli

import net.rubygrapefruit.cli.app.CliAction
import net.rubygrapefruit.cli.app.CliApp
import net.rubygrapefruit.script.interpreter.Interpreter
import net.rubygrapefruit.script.interpreter.StdOut
import net.rubygrapefruit.scripto.bash.BashGenerator
import net.rubygrapefruit.scripto.c.CGenerator
import net.rubygrapefruit.scripto.c.compile.CCompiler
import net.rubygrapefruit.scripto.compiler.Compiler
import net.rubygrapefruit.scripto.ir.TargetRuntime
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
        val bashGenerator = BashGenerator()
        val powershellGenerator = PowershellGenerator()
        val cGenerator = CGenerator()
        val jvmGenerator = JvmGenerator()

        val compiler = Compiler()
        val program = compiler.compile(sourceFile, bashGenerator).get()

        bashGenerator.generate(program, outputDir.file("app.bash"))

        powershellGenerator.generate(program, outputDir.file("app.ps"))

        val cSourceFile = outputDir.file("app.c")
        cGenerator.generate(program, cSourceFile)
        val cCompiler = CCompiler()
        cCompiler.compile(cSourceFile, outputDir.file("app"))

        jvmGenerator.generate(program, outputDir)
    }
}

class RunAction : CliAction() {
    val sourceFile by file(mustExist = true).option("source").required()

    override fun run() {
        val compiler = Compiler()
        val program = compiler.compile(sourceFile, TargetRuntime.NoOp).get()

        val interpreter = Interpreter()
        interpreter.run(program, StdOut.Default)
    }
}

fun main(args: Array<String>) = ScriptoApp().run(args)
