package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.ir.Type
import net.rubygrapefruit.scripto.ir.VariableDeclaration

internal class CVariableDeclaration(
    val name: String,
    val type: Type,
    val value: CExpression,
    val declaredBy: ExternalDeclaration? = null
) : CStatement {
    constructor(declaration: VariableDeclaration, value: CExpression, declaredBy: ExternalDeclaration?) : this(declaration.name, declaration.type, value, declaredBy)

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
