package net.rubygrapefruit.scripto.c

internal sealed interface CStatement {
    /**
     * Visit any declarations used directly by this statement, but not its expressions.
     */
    fun visitOwnDeclarations(visitor: (CDeclaration) -> Unit) {
    }

    /**
     * Visit any expressions used by this statement.
     */
    fun visitExpressions(visitor: (CExpression) -> Unit)
}

internal class CReturnStatement(val value: CExpression) : CStatement {
    override fun visitExpressions(visitor: (CExpression) -> Unit) {
        value.visitExpressions(visitor)
    }
}
