package net.rubygrapefruit.scripto.ir

sealed class FunctionDefinition {
    abstract val parameters: List<Parameter>
}

class Parameter(val type: Type, val isVarargs: Boolean)

class UnitFunctionDefinition(val name: String, override val parameters: List<Parameter>) : FunctionDefinition() {
    constructor(name: String, vararg parameters: Type) : this(name, parameters.map { Parameter(it, false) })
}

sealed class TypedFunctionDefinition : FunctionDefinition() {
    abstract val displayName: String

    abstract val returnType: Type
}

class NamedFunctionDefinition(val name: String, override val returnType: Type, override val parameters: List<Parameter>) : TypedFunctionDefinition() {
    constructor(name: String, returnType: Type, vararg parameters: Type) : this(name, returnType, parameters.map { Parameter(it, false) })

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

class OperatorDefinition(val operator: Operator, override val returnType: Type, override val parameters: List<Parameter>) : TypedFunctionDefinition() {
    constructor(operator: Operator, returnType: Type, left: Type, right: Type) : this(operator, returnType, listOf(Parameter(left, false), Parameter(right, false)))

    override val displayName: String
        get() = "operator '${operator.operator}'"
}
