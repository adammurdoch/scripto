package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.ir.VariableReference

internal sealed interface CExpression {
    /**
     * Visit any headers or code snippets used by this expression.
     */
    fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
    }
}

internal object CNull : CExpression

internal class CString(val value: String) : CExpression

internal class CInt(val value: Int) : CExpression

internal sealed class CBooleanLiteral : CExpression {
    override fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
        visitor(Header.Stdbool)
    }
}

internal data object CTrue : CBooleanLiteral()

internal data object CFalse : CBooleanLiteral()

internal class CVariableReference(val name: String) : CExpression {
    constructor(target: VariableReference) : this(target.target.name)
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

internal class CTemporaryValue(val type: String, val value: CExpression, val declaredBy: ExternalDeclaration? = null) : CExpression {
    constructor(type: TypeSnippet, value: CExpression) : this(type.name, value, type)

    override fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
        if (declaredBy != null) {
            visitor(declaredBy)
        }
        value.visitDeclarations(visitor)
    }
}