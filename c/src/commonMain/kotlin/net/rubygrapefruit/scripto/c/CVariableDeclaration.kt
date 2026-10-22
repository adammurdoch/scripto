package net.rubygrapefruit.scripto.c

internal class CVariableDeclaration(
    val name: String,
    val type: CType,
    val value: CExpression
) : CStatement {
    override fun visitOwnDeclarations(visitor: (CDeclaration) -> Unit) {
        type.visitDeclarations(visitor)
    }

    override fun visitExpressions(visitor: (CExpression) -> Unit) {
        value.visitExpressions(visitor)
    }
}
