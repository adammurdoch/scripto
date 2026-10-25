package net.rubygrapefruit.scripto.ir

sealed interface Expression {
    val type: Type
}

class VariableReference(override val type: Type, val name: String) : Expression

class FunctionCallExpression(val target: TypedFunctionDefinition, val args: List<Expression>) : Expression {
    override val type: Type
        get() = target.returnType
}

class ReceiverFunctionCallExpression(val receiver: Expression, val target: TypedFunctionDefinition, val args: List<Expression>) : Expression {
    override val type: Type
        get() = target.returnType
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
