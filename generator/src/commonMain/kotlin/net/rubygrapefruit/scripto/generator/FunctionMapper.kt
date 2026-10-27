package net.rubygrapefruit.scripto.generator

import net.rubygrapefruit.scripto.ir.*

class FunctionMapper<T>(
    private val mappings: Map<TypedFunctionDefinition, (FunctionCallExpression, FunctionMapper<T>) -> T>,
    private val parameterizedMappings: Map<NamedParameterizedFunctionDefinition, (FunctionCallExpression, RealizedFunctionDefinition, FunctionMapper<T>) -> T>,
    private val receiverMappings: Map<TypedFunctionDefinition, (ReceiverFunctionCallExpression, FunctionMapper<T>) -> T>,
) {
    fun map(expression: FunctionCallExpression): T {
        val target = expression.target
        when (target) {
            is RealizedFunctionDefinition -> {
                val mapping = parameterizedMappings[target.definition] ?: throw IllegalArgumentException("No mapping for ${target.displayName}")
                return mapping(expression, target, this)
            }

            else -> {
                val mapping = mappings[target] ?: throw IllegalArgumentException("No mapping for ${target.displayName}")
                return mapping(expression, this)
            }
        }
    }

    fun map(expression: ReceiverFunctionCallExpression): T {
        val mapping = receiverMappings[expression.target] ?: throw IllegalArgumentException("No mapping for ${expression.target.displayName} of ${expression.receiver.type}")
        return mapping(expression, this)
    }

    interface Builder<T> {
        fun map(target: TypedFunctionDefinition, map: (FunctionCallExpression, FunctionMapper<T>) -> T)

        fun map(target: NamedParameterizedFunctionDefinition, map: (FunctionCallExpression, RealizedFunctionDefinition, FunctionMapper<T>) -> T)

        fun mapReceiver(target: TypedFunctionDefinition, map: (ReceiverFunctionCallExpression, FunctionMapper<T>) -> T)
    }

    private class DefaultBuilder<T> : Builder<T> {
        private val mappings = mutableMapOf<TypedFunctionDefinition, (FunctionCallExpression, FunctionMapper<T>) -> T>()
        private val parameterizedMappings = mutableMapOf<NamedParameterizedFunctionDefinition, (FunctionCallExpression, RealizedFunctionDefinition, FunctionMapper<T>) -> T>()
        private val receiverMappings = mutableMapOf<TypedFunctionDefinition, (ReceiverFunctionCallExpression, FunctionMapper<T>) -> T>()

        override fun map(target: TypedFunctionDefinition, map: (FunctionCallExpression, FunctionMapper<T>) -> T) {
            mappings[target] = map
        }

        override fun map(target: NamedParameterizedFunctionDefinition, map: (FunctionCallExpression, RealizedFunctionDefinition, FunctionMapper<T>) -> T) {
            parameterizedMappings[target] = map
        }

        override fun mapReceiver(target: TypedFunctionDefinition, map: (ReceiverFunctionCallExpression, FunctionMapper<T>) -> T) {
            receiverMappings[target] = map
        }

        fun build() = FunctionMapper(mappings, parameterizedMappings, receiverMappings)
    }

    companion object {
        fun <T> of(builder: Builder<T>.() -> Unit): FunctionMapper<T> {
            val mappings = DefaultBuilder<T>()
            mappings.builder()
            return mappings.build()
        }
    }
}