package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.generator.Declaration

internal class BashGeneratedFunction(val name: String, val body: String) : Declaration<BashGeneratedFunction> {
    override fun visitOwnDeclarations(visitor: (BashGeneratedFunction) -> Unit) {
    }
}