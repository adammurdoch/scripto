package net.rubygrapefruit.scripto.c

internal class CStatementBuilder {
    val statements: List<CStatement>
        field = mutableListOf<CStatement>()

    fun add(statement: CFunctionCall) {
        for (expression in statement.arguments) {
            expression.visitSetup { add(it) }
        }
        statements.add(statement)
        for (expression in statement.arguments) {
            expression.visitCleanup { add(it) }
        }
    }

    fun add(statement: CVariableDeclaration) {
        statement.value.visitSetup { add(it) }
        statements.add(statement)
        statement.value.visitCleanup { add(it) }
    }

    private fun add(statement: CStatement) {
        when (statement) {
            is CFunctionCall -> add(statement)
            is CVariableDeclaration -> add(statement)
        }
    }
}