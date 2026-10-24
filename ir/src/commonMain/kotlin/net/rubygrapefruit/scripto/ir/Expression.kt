package net.rubygrapefruit.scripto.ir

sealed interface Expression {
    val type: Type

    /**
     * Visits this expression and all expressions it includes.
     */
    fun visitExpressions(visitor: (Expression) -> Unit) {
        visitor(this)
    }
}

class VariableReference(val target: VariableDeclaration) : Expression {
    override val type: Type
        get() = target.type
}

class FunctionCallExpression(val target: TypedFunctionDefinition, val args: List<Expression>) : Expression {
    override val type: Type
        get() = target.returnType

    override fun visitExpressions(visitor: (Expression) -> Unit) {
        for (arg in args) {
            arg.visitExpressions(visitor)
        }
        visitor(this)
    }
}

class ReceiverFunctionCallExpression(val receiver: Expression, val target: TypedFunctionDefinition, val args: List<Expression>) : Expression {
    override val type: Type
        get() = target.returnType

    override fun visitExpressions(visitor: (Expression) -> Unit) {
        for (arg in args) {
            arg.visitExpressions(visitor)
        }
        visitor(this)
    }
}

sealed interface Literal : Expression

class StringLiteral(val value: String) : Literal {
    override val type: Type
        get() = Type.String
}

class IntLiteral(val value: Int) : Literal {
    override val type: Type
        get() = Type.Int
}

class BooleanLiteral(val value: Boolean) : Literal {
    override val type: Type
        get() = Type.Boolean

    companion object {
        const val TRUE = "true"
        const val FALSE = "false"
    }
}
