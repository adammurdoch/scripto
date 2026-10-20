package net.rubygrapefruit.scripto.ir

sealed interface FunctionKind

sealed interface FunctionDefinition : FunctionKind {
    val displayName: String

    val parameters: List<Parameter>
}

sealed interface ParameterFunction {
    fun evaluate(arg: Type): Parameter

    class UseFunctionParameter(val isVarargs: Boolean) : ParameterFunction {
        override fun evaluate(arg: Type): Parameter {
            return Parameter(arg, isVarargs)
        }
    }
}

class Parameter(val type: Type, val isVarargs: Boolean)

class UnitFunctionDefinition(val name: String, override val parameters: List<Parameter>) : FunctionDefinition {
    constructor(name: String, vararg parameters: Type) : this(name, parameters.map { Parameter(it, false) })

    override val displayName: String
        get() = "function $name()"
}

sealed interface TypedFunctionDefinition : FunctionDefinition {
    val returnType: Type
}

class NamedFunctionDefinition(val name: String, override val returnType: Type, override val parameters: List<Parameter>) : TypedFunctionDefinition {
    constructor(name: String, returnType: Type, vararg parameters: Type) : this(name, returnType, parameters.map { Parameter(it, false) })

    override val displayName: String
        get() = "function $name()"
}

class NamedParameterizedFunctionDefinition(val name: String, val returnType: TypeFunction, val parameters: List<ParameterFunction>) : FunctionKind {
    fun evaluate(arg: Type): RealizedFunctionDefinition {
        return RealizedFunctionDefinition(this, returnType.evaluate(arg), parameters.map { it.evaluate(arg) })
    }
}

class RealizedFunctionDefinition(val definition: NamedParameterizedFunctionDefinition, override val returnType: Type, override val parameters: List<Parameter>) :
    TypedFunctionDefinition {
    override val displayName: String
        get() = "function ${definition.name}()"
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

class OperatorDefinition(val operator: Operator, override val returnType: Type, override val parameters: List<Parameter>) : TypedFunctionDefinition {
    constructor(operator: Operator, returnType: Type, left: Type, right: Type) : this(operator, returnType, listOf(Parameter(left, false), Parameter(right, false)))

    override val displayName: String
        get() = "operator '${operator.operator}'"
}
