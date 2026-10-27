package net.rubygrapefruit.scripto.node

internal class VariableMapping {
    fun map(reference: JsVariableReference): JsExpression? {
        return if (reference.name == "args") {
            JsFunctionCall(JsVariableReference("process.argv"), "slice", listOf(JsInt(2)))
        } else {
            null
        }
    }
}