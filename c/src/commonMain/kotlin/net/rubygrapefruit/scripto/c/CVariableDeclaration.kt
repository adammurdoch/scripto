package net.rubygrapefruit.scripto.c

internal class CVariableDeclaration(
    val name: String,
    val type: String,
    val value: CExpression,
    val declaredBy: ExternalDeclaration? = null
) : CStatement {
    override fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
        if (declaredBy != null) {
            visitor(declaredBy)
        }
        value.visitDeclarations(visitor)
    }

    override fun visitExpressions(visitor: (CExpression) -> Unit) {
        value.visitExpressions(visitor)
    }
}
