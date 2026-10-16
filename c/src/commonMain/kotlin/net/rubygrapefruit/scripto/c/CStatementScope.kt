package net.rubygrapefruit.scripto.c

internal class CStatementScope {
    private val counts = mutableMapOf<String, Int>()

    fun newVarName(base: String): CVariableReference {
        val count = counts[base] ?: 1
        counts[base] = count + 1
        return if (count == 1) {
            CVariableReference(base)
        } else {
            CVariableReference("$base$count")
        }
    }
}