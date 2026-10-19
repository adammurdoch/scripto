package net.rubygrapefruit.scripto.ir

sealed class FunctionDefinition {
    abstract val args: List<IncomingType>
}

class UnitFunctionDefinition(val name: String, override val args: List<IncomingType>) : FunctionDefinition()

sealed class TypedFunctionDefinition : FunctionDefinition() {
    abstract val displayName: String

    abstract val returnType: Type
}

class NamedFunctionDefinition(val name: String, override val returnType: Type, override val args: List<IncomingType>) : TypedFunctionDefinition() {
    override val displayName: String
        get() = "function $name()"
}

sealed class Operator {
    abstract val operator: String

    data object Plus : Operator() {
        override val operator: String
            get() = "+"
    }

    data object Equality : Operator() {
        override val operator: String
            get() = "=="
    }
}

class OperatorDefinition(val operator: Operator, override val returnType: Type, override val args: List<Type>) : TypedFunctionDefinition() {
    override val displayName: String
        get() = "operator '${operator.operator}'"
}
