package net.rubygrapefruit.scripto.ir

sealed class FunctionDefinition {
    abstract val args: List<IncomingType>
}

data class VoidFunctionDefinition(val name: String, override val args: List<IncomingType>) : FunctionDefinition()

data class TypedFunctionDefinition(val name: String, val returnType: Type, override val args: List<IncomingType>) : FunctionDefinition()

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

data class OperatorDefinition(val operator: Operator, val returnType: Type, override val args: List<Type>) : FunctionDefinition()
