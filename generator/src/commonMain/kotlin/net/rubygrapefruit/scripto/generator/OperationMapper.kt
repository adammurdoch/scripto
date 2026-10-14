package net.rubygrapefruit.scripto.generator

import net.rubygrapefruit.scripto.ir.FunctionCall
import net.rubygrapefruit.scripto.ir.Operation
import net.rubygrapefruit.scripto.ir.VariableDeclaration
import net.rubygrapefruit.scripto.ir.VoidFunctionDefinition

class OperationMapper<T> private constructor(
    private val mappings: Map<String, List<Mapping<T>>>,
    private val varMap: ((VariableDeclaration) -> List<T>)?
) {
    fun map(operations: List<Operation>): List<T> {
        val result = mutableListOf<T>()
        operations.forEach { operation ->
            when (operation) {
                is FunctionCall -> {
                    val candidates = mappings[operation.target.name]
                    val mapping = candidates?.find { it.definition.args == operation.target.args }
                    if (mapping == null) {
                        throw IllegalStateException("No mapping for function ${operation.target}")
                    }
                    result.add(mapping.map(operation))
                }

                is VariableDeclaration -> {
                    if (varMap != null) {
                        result.addAll(varMap(operation))
                    }
                }
            }
        }
        return result
    }

    interface Builder<T> {
        fun mapFun(definition: VoidFunctionDefinition, map: (FunctionCall) -> T)

        fun mapVar(map: (VariableDeclaration) -> T)

        fun flatMapVar(map: (VariableDeclaration) -> List<T>)
    }

    private class DefaultBuilder<T> : Builder<T> {
        val mappings = mutableMapOf<String, MutableList<Mapping<T>>>()
        var varMapping: ((VariableDeclaration) -> List<T>)? = null

        override fun mapFun(definition: VoidFunctionDefinition, map: (FunctionCall) -> T) {
            val mappingsForName = mappings.getOrPut(definition.name) { mutableListOf() }
            mappingsForName.add(Mapping(definition, map))
        }

        override fun mapVar(map: (VariableDeclaration) -> T) {
            varMapping = { listOf(map(it)) }
        }

        override fun flatMapVar(map: (VariableDeclaration) -> List<T>) {
            varMapping = map
        }

        fun build() = OperationMapper(mappings, varMapping)
    }

    companion object {
        fun <T> of(builder: Builder<T>.() -> Unit): OperationMapper<T> {
            val mappings = DefaultBuilder<T>()
            mappings.builder()
            return mappings.build()
        }
    }

    private class Mapping<T>(val definition: VoidFunctionDefinition, val map: (FunctionCall) -> T)
}