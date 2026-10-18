package net.rubygrapefruit.scripto.generator

class StatementScope {
    private val counts = mutableMapOf<String, Int>()

    fun newVarName(base: String): String {
        val count = counts[base] ?: 1
        counts[base] = count + 1
        return if (count == 1) {
            "_$base"
        } else {
            "_${base}_$count"
        }
    }
}