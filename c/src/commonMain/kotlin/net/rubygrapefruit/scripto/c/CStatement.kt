package net.rubygrapefruit.scripto.c

internal sealed interface CStatement {
    /**
     * Visit any declarations used by this statement.
     */
    fun visitDeclarations(visitor: (CDeclaration) -> Unit)

    /**
     * Visit any expressions used by this statement.
     */
    fun visitExpressions(visitor: (CExpression) -> Unit)
}

internal class CReturnStatement(val value: CExpression) : CStatement {
    override fun visitDeclarations(visitor: (CDeclaration) -> Unit) {
        value.visitDeclarations(visitor)
    }

    override fun visitExpressions(visitor: (CExpression) -> Unit) {
        value.visitExpressions(visitor)
    }
}
