package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.ir.VariableReference

internal sealed interface CExpression {
    /**
     * Visit any headers or code snippets used by this expression.
     */
    fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
    }

    /**
     * Visit any setup statements required by this expression.
     */
    fun visitSetup(visitor: (CStatement) -> Unit) {
    }

    /**
     * Visit any cleanup statements required by this expression.
     */
    fun visitCleanup(visitor: (CStatement) -> Unit) {
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

    override fun visitSetup(visitor: (CStatement) -> Unit) {
        left.visitSetup(visitor)
        right.visitSetup(visitor)
    }

    override fun visitCleanup(visitor: (CStatement) -> Unit) {
        left.visitCleanup(visitor)
        right.visitCleanup(visitor)
    }
}

internal class CTernaryExpression(val condition: CExpression, val left: CExpression, val right: CExpression) : CExpression {
    override fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
        condition.visitDeclarations(visitor)
        left.visitDeclarations(visitor)
        right.visitDeclarations(visitor)
    }

    override fun visitSetup(visitor: (CStatement) -> Unit) {
        condition.visitSetup(visitor)
        left.visitSetup(visitor)
        right.visitSetup(visitor)
    }

    override fun visitCleanup(visitor: (CStatement) -> Unit) {
        condition.visitCleanup(visitor)
        left.visitCleanup(visitor)
        right.visitCleanup(visitor)
    }
}

/**
 * An expression that uses the result of some statement(s).
 */
internal class CPreCalculatedExpression(val setup: List<CStatement>, val value: CVariableReference, val cleanup: List<CStatement>) : CExpression {
    override fun visitSetup(visitor: (CStatement) -> Unit) {
        setup.forEach(visitor)
    }

    override fun visitCleanup(visitor: (CStatement) -> Unit) {
        cleanup.forEach(visitor)
    }
}
