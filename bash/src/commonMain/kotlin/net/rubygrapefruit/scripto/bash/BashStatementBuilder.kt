package net.rubygrapefruit.scripto.bash

internal class BashStatementBuilder {
    val statements: List<BashStatement>
        field = mutableListOf<BashStatement>()

    fun add(statement: BashStatement) {
        statement.visitDependencies {
            add(it)
        }
        statements.add(statement)
    }
}