package net.rubygrapefruit.scripto.ir

sealed interface Operation {
    fun visitExpressions(visitor: (Expression) -> Unit)
}

class VariableDeclaration(val name: String, val value: Expression) : Operation {
    val type: Type
        get() = value.type

    override fun visitExpressions(visitor: (Expression) -> Unit) {
        value.visitExpressions(visitor)
    }
}

class UnitFunctionCall(val target: UnitFunctionDefinition, val arguments: List<Expression>) : Operation {
    override fun visitExpressions(visitor: (Expression) -> Unit) {
        for (argument in arguments) {
            argument.visitExpressions(visitor)
        }
    }
}
