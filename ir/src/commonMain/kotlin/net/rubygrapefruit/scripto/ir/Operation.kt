package net.rubygrapefruit.scripto.ir

sealed interface Operation

class VariableDeclaration(val name: String, val value: Expression) : Operation {
    val type: Type
        get() = value.type
}

class FunctionCall(val target: UnitFunctionDefinition, val arguments: List<Expression>) : Operation
