package net.rubygrapefruit.scripto.c

internal sealed interface CStatement {
    fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit)
}
