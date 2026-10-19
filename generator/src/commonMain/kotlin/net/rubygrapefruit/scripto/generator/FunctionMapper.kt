package net.rubygrapefruit.scripto.generator

import net.rubygrapefruit.scripto.ir.FunctionCallExpression
import net.rubygrapefruit.scripto.ir.TypedFunctionDefinition

class FunctionMapper<T>(
    private val mappings: Map<TypedFunctionDefinition, (FunctionCallExpression, FunctionMapper<T>) -> T>
) {
    fun map(expression: FunctionCallExpression): T {
        val mapping = mappings[expression.target] ?: throw IllegalArgumentException("Unknown ${expression.target.displayName}")
        return mapping(expression, this)
    }

    interface Builder<T> {
        fun map(target: TypedFunctionDefinition, map: (FunctionCallExpression, FunctionMapper<T>) -> T)
    }

    private class DefaultBuilder<T> : Builder<T> {
        private val mappings = mutableMapOf<TypedFunctionDefinition, (FunctionCallExpression, FunctionMapper<T>) -> T>()

        override fun map(target: TypedFunctionDefinition, map: (FunctionCallExpression, FunctionMapper<T>) -> T) {
            mappings[target] = map
        }

        fun build() = FunctionMapper(mappings)
    }

    companion object {
        fun <T> of(builder: Builder<T>.() -> Unit): FunctionMapper<T> {
            val mappings = DefaultBuilder<T>()
            mappings.builder()
            return mappings.build()
        }
    }
}