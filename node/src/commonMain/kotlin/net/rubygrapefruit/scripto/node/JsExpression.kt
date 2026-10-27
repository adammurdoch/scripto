package net.rubygrapefruit.scripto.node

internal sealed interface JsExpression {
    fun visitOwnDeclarations(visitor: (JsDeclaration) -> Unit) {
    }

    fun visitExpressions(visitor: (JsExpression) -> Unit) {
        visitor(this)
    }
}

internal sealed class JsCompositeExpression : JsExpression {
    override fun visitExpressions(visitor: (JsExpression) -> Unit) {
        visitChildren { child ->
            child.visitExpressions(visitor)
        }
        visitor(this)
    }

    protected abstract fun visitChildren(visitor: (JsExpression) -> Unit)
}

internal class JsString(val value: String) : JsExpression

internal class JsInt(val value: Int) : JsExpression

internal class JsBoolean(val value: Boolean) : JsExpression

internal class JsVariableReference(val name: String) : JsExpression

internal class JsInfix(val left: JsExpression, val operator: String, val right: JsExpression) : JsCompositeExpression() {
    override fun visitChildren(visitor: (JsExpression) -> Unit) {
        visitor(left)
        visitor(right)
    }
}

internal class JsArrayLiteral(val elements: List<JsExpression>) : JsCompositeExpression() {
    override fun visitChildren(visitor: (JsExpression) -> Unit) {
        elements.forEach(visitor)
    }
}

internal class JsLambda(val argName: String, val value: JsExpression) : JsCompositeExpression() {
    override fun visitChildren(visitor: (JsExpression) -> Unit) {
        visitor(value)
    }
}
