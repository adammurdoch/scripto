package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.generator.ExpressionWithDependencies
import net.rubygrapefruit.scripto.ir.VariableReference

internal sealed interface CExpression : ExpressionWithDependencies<CStatement> {
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

    override fun visitDependencies(visitor: (CStatement) -> Unit) {
        left.visitDependencies(visitor)
        right.visitDependencies(visitor)
    }
}

internal class CTernaryExpression(val condition: CExpression, val left: CExpression, val right: CExpression) : CExpression {
    override fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
        condition.visitDeclarations(visitor)
        left.visitDeclarations(visitor)
        right.visitDeclarations(visitor)
    }

    override fun visitDependencies(visitor: (CStatement) -> Unit) {
        condition.visitDependencies(visitor)
        left.visitDependencies(visitor)
        right.visitDependencies(visitor)
    }
}

/**
 * An expression that uses the result of some statement(s).
 */
internal class CPreCalculatedExpression(val setup: List<CStatement>, val value: CVariableReference, val cleanup: List<CStatement>) : CExpression {
    override fun visitDependencies(visitor: (CStatement) -> Unit) {
        setup.forEach(visitor)
    }
}
