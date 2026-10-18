package net.rubygrapefruit.scripto.bash

internal class BashStatementBuilder {
    val statements: List<BashStatement>
        field = mutableListOf<BashStatement>()

    fun add(statement: BashCommandInvocation) {
        statement.visitDependencies {
            add(it)
        }
        statements.add(statement)
    }

    fun add(statement: BashVariableAssignment) {
        statement.visitDependencies {
            add(it)
        }
        statements.add(statement)
    }

    private fun add(statement: BashStatement) {
        statement.visitDependencies {
            add(it)
        }
        statements.add(statement)
    }
}