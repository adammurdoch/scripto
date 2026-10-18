package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.generator.StatementScope

internal class CStatementScope {
    private val vars = StatementScope()

    fun newVarName(base: String): CVariableReference {
        val name = vars.newVarName(base)
        return CVariableReference(name)
    }
}