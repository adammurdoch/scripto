package net.rubygrapefruit.scripto.node

import net.rubygrapefruit.scripto.generator.Declaration

internal sealed interface JsDeclaration : Declaration<JsDeclaration> {
    override fun visitOwnDeclarations(visitor: (JsDeclaration) -> Unit) {
    }
}

internal sealed class JsRequire(val name: String, val module: String) : JsDeclaration {
    object Fs: JsRequire("fs", "node:fs")
}