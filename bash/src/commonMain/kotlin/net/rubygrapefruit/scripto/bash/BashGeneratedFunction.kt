package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.generator.Declaration
import net.rubygrapefruit.scripto.generator.SourceGenerator

internal class BashGeneratedFunction(val name: String, val returnVar: String, val body: String) : Declaration<BashGeneratedFunction> {
    override fun visitOwnDeclarations(visitor: (BashGeneratedFunction) -> Unit) {
    }

    fun SourceGenerator.append() {
        append("function ")
        append(name)
        appendln(" {")
        for (line in body.lines()) {
            append("    ")
            appendln(line)
        }
        appendln("}")
        appendln()
    }
}