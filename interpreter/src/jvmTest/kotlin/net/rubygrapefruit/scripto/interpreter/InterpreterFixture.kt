package net.rubygrapefruit.scripto.interpreter

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.script.interpreter.Interpreter
import net.rubygrapefruit.script.interpreter.StdOut
import net.rubygrapefruit.scripto.ir.Program
import net.rubygrapefruit.scripto.test.AppFixture
import java.io.StringWriter

class InterpreterFixture(workDir: Directory) : AppFixture(workDir) {
    override fun generateAndRun(program: Program): String {
        val stdout = CollectingStdOut()
        val interpreter = Interpreter()
        interpreter.run(program, stdout)
        return stdout.writer.toString()
    }

    private class CollectingStdOut : StdOut {
        val writer = StringWriter()

        override fun println(text: String) {
            writer.appendLine(text)
        }
    }
}