package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.ir.VariableReference

internal sealed interface CExpression {
    fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
    }
}

internal class CString(val value: String) : CExpression

internal class CInt(val value: Int) : CExpression

internal sealed class CBooleanLiteral : CExpression {
    override fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
        visitor(Header.Stdbool)
    }
}

internal data object CTrue : CBooleanLiteral()

internal data object CFalse : CBooleanLiteral()

internal class CVariableReference(val target: VariableReference) : CExpression {
    val name: String
        get() = target.target.name
}

internal class CInfixOperator(val left: CExpression, val operator: String, val right: CExpression) : CExpression {
    override fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
        left.visitDeclarations(visitor)
        right.visitDeclarations(visitor)
    }
}

internal class CTernaryExpression(val condition: CExpression, val left: CExpression, val right: CExpression) : CExpression {
    override fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
        condition.visitDeclarations(visitor)
        left.visitDeclarations(visitor)
        right.visitDeclarations(visitor)
    }
}
