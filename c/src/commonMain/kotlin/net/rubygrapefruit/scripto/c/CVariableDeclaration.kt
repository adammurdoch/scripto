package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.ir.Type
import net.rubygrapefruit.scripto.ir.VariableDeclaration

internal class CVariableDeclaration(
    val declaration: VariableDeclaration,
    val value: CExpression,
    val declaredBy: ExternalDeclaration?
) : CStatement {
    val name: String
        get() = declaration.name

    val type: Type
        get() = declaration.type

    override fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
        if (declaredBy != null) {
            visitor(declaredBy)
        }
        value.visitDeclarations(visitor)
    }
}
