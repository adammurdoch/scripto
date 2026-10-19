package net.rubygrapefruit.scripto.generator

import net.rubygrapefruit.scripto.ir.FunctionCall
import net.rubygrapefruit.scripto.ir.Operation
import net.rubygrapefruit.scripto.ir.UnitFunctionDefinition
import net.rubygrapefruit.scripto.ir.VariableDeclaration

/**
 * Maps a sequence of [Operation] to a sequence of type [T].
 */
class OperationMapper<T> private constructor(
    private val mappings: Map<UnitFunctionDefinition, Context<T>.(FunctionCall) -> Unit>,
    private val varMap: Context<T>.(VariableDeclaration) -> Unit,
    private val collectDependencies: Context<T>.(T) -> Unit
) {
    fun map(operations: List<Operation>): List<T> {
        val result = mutableListOf<T>()
        val context = DefaultContext(result, collectDependencies)
        for (operation in operations) {
            map(operation, context)
        }
        return result
    }

    private fun map(operation: Operation, context: Context<T>) {
        when (operation) {
            is FunctionCall -> {
                val mapping = mappings[operation.target] ?: throw IllegalArgumentException("No mapping for function ${operation.target.name}()")
                mapping(context, operation)
            }

            is VariableDeclaration -> {
                varMap(context, operation)
            }
        }
    }

    interface Context<T> {
        fun add(operation: T)

        fun addDependencies(operation: ExpressionWithDependencies<T>)

        fun addDependencies(operations: List<ExpressionWithDependencies<T>>)
    }

    private class DefaultContext<T>(val result: MutableList<T>, val collectDependencies: Context<T>.(T) -> Unit) : Context<T> {
        override fun add(operation: T) {
            collectDependencies(operation)
            result.add(operation)
        }

        override fun addDependencies(operation: ExpressionWithDependencies<T>) {
            operation.visitDependencies(::add)
        }

        override fun addDependencies(operations: List<ExpressionWithDependencies<T>>) {
            operations.forEach(::addDependencies)
        }
    }

    interface Builder<T> {
        fun map(definition: UnitFunctionDefinition, map: (FunctionCall) -> T)

        fun mapN(definition: UnitFunctionDefinition, map: Context<T>.(FunctionCall) -> Unit)

        fun map(map: (VariableDeclaration) -> T)

        fun mapN(map: Context<T>.(VariableDeclaration) -> Unit)

        fun collectDependencies(map: Context<T>.(T) -> Unit)
    }

    private class DefaultBuilder<T> : Builder<T> {
        private val mappings = mutableMapOf<UnitFunctionDefinition, Context<T>.(FunctionCall) -> Unit>()
        private var varMapping: Context<T>.(VariableDeclaration) -> Unit = { throw IllegalArgumentException("No mapping for variable declaration") }
        private var collectDependencies: Context<T>.(T) -> Unit = {}

        override fun map(definition: UnitFunctionDefinition, map: (FunctionCall) -> T) {
            mappings[definition] = { add(map(it)) }
        }

        override fun mapN(definition: UnitFunctionDefinition, map: Context<T>.(FunctionCall) -> Unit) {
            mappings[definition] = map
        }

        override fun map(map: (VariableDeclaration) -> T) {
            varMapping = { add(map(it)) }
        }

        override fun mapN(map: Context<T>.(VariableDeclaration) -> Unit) {
            varMapping = map
        }

        override fun collectDependencies(map: Context<T>.(T) -> Unit) {
            collectDependencies = map
        }

        fun build() = OperationMapper(mappings, varMapping, collectDependencies)
    }

    companion object {
        fun <T> of(builder: Builder<T>.() -> Unit): OperationMapper<T> {
            val mappings = DefaultBuilder<T>()
            mappings.builder()
            return mappings.build()
        }
    }
}