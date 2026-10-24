package net.rubygrapefruit.scripto.generator

@OptIn(ExperimentalStdlibApi::class)
class VariableState {
    private val counts = mutableMapOf<String, Int>()
    private val expressions = mutableMapOf<TemporaryValue, String>()

    fun assign(expression: TemporaryValue): String {
        val varName = nameFor(expression.nameHint)
        expressions[expression] = varName
        return varName
    }

    fun varFor(expression: TemporaryValue): String {
        return expressions.getValue(expression)
    }

    private fun nameFor(nameHint: String): String {
        val count = counts.getOrPutIfNull(nameHint) { 1 }
        counts[nameHint] = count + 1
        return if (count == 1) {
            "_$nameHint"
        } else {
            "_$nameHint$count"
        }
    }
}
