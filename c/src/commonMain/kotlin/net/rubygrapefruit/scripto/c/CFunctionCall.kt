package net.rubygrapefruit.scripto.c

internal class CFunctionCall(
    val name: String,
    val arguments: List<CExpression>,
    val declaredBy: ExternalDeclaration? = null
) : CStatement, CExpression {
    override fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
        if (declaredBy != null) {
            visitor(declaredBy)
        }
        arguments.forEach { it.visitDeclarations(visitor) }
    }

    override fun visitSetup(visitor: (CStatement) -> Unit) {
        arguments.forEach { it.visitSetup(visitor) }
    }

    override fun visitCleanup(visitor: (CStatement) -> Unit) {
        arguments.forEach { it.visitCleanup(visitor) }
    }
}
