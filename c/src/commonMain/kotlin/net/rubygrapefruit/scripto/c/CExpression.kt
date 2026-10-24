package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.generator.TemporaryValue
import net.rubygrapefruit.scripto.ir.VariableReference

internal sealed interface CExpression {
    /**
     * Visit any declarations used directly by this expression, but not its nested expressions.
     */
    fun visitOwnDeclarations(visitor: (CDeclaration) -> Unit) {
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
    override fun visitOwnDeclarations(visitor: (CDeclaration) -> Unit) {
        visitor(Header.Stdbool)
    }

    companion object {
        fun of(value: Boolean) = if (value) CTrue else CFalse
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

    protected abstract fun visitChildren(visitor: (CExpression) -> Unit)
}

internal class CPointer(val value: CExpression) : CCompositeExpression() {
    override fun visitChildren(visitor: (CExpression) -> Unit) {
        visitor(value)
    }
}

internal class CFunctionPointer(val function: CFunctionDeclaration) : CExpression {
    override fun visitOwnDeclarations(visitor: (CDeclaration) -> Unit) {
        visitor(function)
    }
}

internal class CCast(val type: CType, val value: CExpression) : CCompositeExpression() {
    override fun visitOwnDeclarations(visitor: (CDeclaration) -> Unit) {
        type.visitDeclarations(visitor)
    }

    override fun visitChildren(visitor: (CExpression) -> Unit) {
        visitor(value)
    }
}

internal class CStructField(val struct: CExpression, val fieldName: String) : CCompositeExpression() {
    override fun visitChildren(visitor: (CExpression) -> Unit) {
        visitor(struct)
    }
}

internal class CArrayIndex(val array: CExpression, val index: CExpression) : CCompositeExpression() {
    override fun visitChildren(visitor: (CExpression) -> Unit) {
        visitor(array)
        visitor(index)
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

internal class CInitializer(val elements: List<CExpression>) : CCompositeExpression() {
    override fun visitChildren(visitor: (CExpression) -> Unit) {
        elements.forEach(visitor)
    }
}

/**
 * An expression that must be placed into a variable.
 */
internal class CTemporaryValue(val type: CType, override val nameHint: String, val value: CExpression) : CCompositeExpression(), TemporaryValue {
    override fun visitChildren(visitor: (CExpression) -> Unit) {
        visitor(value)
    }

    override fun visitOwnDeclarations(visitor: (CDeclaration) -> Unit) {
        type.visitDeclarations(visitor)
    }
}