package net.rubygrapefruit.scripto.generator

interface ExpressionWithDependencies<out S> {
    fun visitDependencies(visitor: (S) -> Unit) {
    }
}