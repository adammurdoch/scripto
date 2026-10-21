package net.rubygrapefruit.scripto.c

internal class CVariableDeclaration(
    val name: String,
    val type: CType,
    val value: CExpression
) : CStatement {
    override fun visitDeclarations(visitor: (CDeclaration) -> Unit) {
        type.visitDeclarations(visitor)
        value.visitDeclarations(visitor)
    }

    override fun visitExpressions(visitor: (CExpression) -> Unit) {
        value.visitExpressions(visitor)
    }
}
