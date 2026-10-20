package net.rubygrapefruit.scripto.c

internal sealed interface CStatement {
    /**
     * Visit any headers or code snippets used by this statement.
     */
    fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit)

    /**
     * Visit any expressions used by this statement.
     */
    fun visitExpressions(visitor: (CExpression) -> Unit)
}
