package net.rubygrapefruit.scripto.ir

interface TargetRuntime {
    fun inspect(function: FunctionDefinition)

    object NoOp : TargetRuntime {
        override fun inspect(function: FunctionDefinition) {
        }
    }
}