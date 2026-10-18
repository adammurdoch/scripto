package net.rubygrapefruit.scripto.c

internal class CStatementBuilder {
    val statements: List<CStatement>
        field = mutableListOf<CStatement>()

    fun add(statement: CFunctionCall) {
        for (expression in statement.arguments) {
            expression.visitDependencies { add(it) }
        }
        statements.add(statement)
    }

    fun add(statement: CVariableDeclaration) {
        statement.value.visitDependencies { add(it) }
        statements.add(statement)
    }

    private fun add(statement: CStatement) {
        when (statement) {
            is CFunctionCall -> add(statement)
            is CVariableDeclaration -> add(statement)
        }
    }
}