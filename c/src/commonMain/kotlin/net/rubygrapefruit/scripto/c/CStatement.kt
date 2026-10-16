package net.rubygrapefruit.scripto.c

internal sealed interface CStatement {
    /**
     * Visit any headers or code snippets used by this expression.
     */
    fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit)
}
