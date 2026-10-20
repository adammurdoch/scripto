package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.ir.VariableReference

internal sealed interface CExpression {
    /**
     * Visit any headers or code snippets used by this expression, including any nested expressions.
     */
    fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
    }

    /**
     * Visits this expression plus any nested expressions. Expressions are visited in dependency order.
     */
    fun visitExpressions(visitor: (CExpression) -> Unit) {
        visitor(this)
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

internal sealed class CCompositeExpression : CExpression {
    override fun visitExpressions(visitor: (CExpression) -> Unit) {
        visitChildren { child ->
            child.visitExpressions(visitor)
        }
        visitor(this)
    }

    override fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
        visitOwnDeclarations(visitor)
        visitChildren { child ->
            child.visitDeclarations(visitor)
        }
    }

    protected abstract fun visitChildren(visitor: (CExpression) -> Unit)

    protected open fun visitOwnDeclarations(visitor: (ExternalDeclaration) -> Unit) {
    }
}

internal class CInfixOperator(val left: CExpression, val operator: String, val right: CExpression) : CCompositeExpression() {
    override fun visitChildren(visitor: (CExpression) -> Unit) {
        visitor(left)
        visitor(right)
    }
}

internal class CTernaryExpression(val condition: CExpression, val left: CExpression, val right: CExpression) : CCompositeExpression() {
    override fun visitChildren(visitor: (CExpression) -> Unit) {
        visitor(condition)
        visitor(left)
        visitor(right)
    }
}

internal class CArrayInitializer(val elements: List<CExpression>): CCompositeExpression() {
    override fun visitChildren(visitor: (CExpression) -> Unit) {
        elements.forEach(visitor)
    }
}

internal class CTemporaryValue(val type: String, val value: CExpression, val declaredBy: ExternalDeclaration? = null) : CCompositeExpression() {
    constructor(type: TypeSnippet, value: CExpression) : this(type.name, value, type)

    override fun visitChildren(visitor: (CExpression) -> Unit) {
        visitor(value)
    }

    override fun visitOwnDeclarations(visitor: (ExternalDeclaration) -> Unit) {
        if (declaredBy != null) {
            visitor(declaredBy)
        }
    }
}