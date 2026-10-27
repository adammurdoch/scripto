package net.rubygrapefruit.scripto.node

internal sealed interface JsExpression {
    fun visitOwnDeclarations(visitor: (JsDeclaration) -> Unit) {
    }

    fun visitExpressions(visitor: (JsExpression) -> Unit) {
        visitor(this)
    }
}

internal class JsString(val value: String) : JsExpression

internal class JsInt(val value: Int) : JsExpression

internal class JsBoolean(val value: Boolean) : JsExpression

internal class JsVariableReference(val name: String) : JsExpression

internal class JsInfix(val left: JsExpression, val operator: String, val right: JsExpression) : JsExpression {
    override fun visitExpressions(visitor: (JsExpression) -> Unit) {
        left.visitExpressions(visitor)
        right.visitExpressions(visitor)
        visitor(this)
    }
}

internal class JsArrayLiteral(val elements: List<JsExpression>) : JsExpression {
    override fun visitExpressions(visitor: (JsExpression) -> Unit) {
        for (element in elements) {
            element.visitExpressions(visitor)
        }
        visitor(this)
    }
}
