package net.rubygrapefruit.scripto.ir

class Program(val operations: List<Operation>) {
    fun visitExpressions(visitor: (Expression) -> Unit) {
        for (operation in operations) {
            operation.visitExpressions(visitor)
        }
    }
}
