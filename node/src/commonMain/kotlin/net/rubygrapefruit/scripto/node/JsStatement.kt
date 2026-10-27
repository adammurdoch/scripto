package net.rubygrapefruit.scripto.node

internal sealed interface JsStatement {
    fun visitExpressions(visitor: (JsExpression) -> Unit)
}

internal class JsVariableDeclaration(val name: String, val value: JsExpression) : JsStatement {
    override fun visitExpressions(visitor: (JsExpression) -> Unit) {
        value.visitExpressions(visitor)
    }
}

internal class JsFunctionCall(val receiver: JsExpression, val name: String, val arguments: List<JsExpression>, val declaredBy: JsDeclaration? = null) :
    JsCompositeExpression(), JsStatement {
    override fun visitOwnDeclarations(visitor: (JsDeclaration) -> Unit) {
        if (declaredBy != null) {
            visitor(declaredBy)
        }
    }

    override fun visitChildren(visitor: (JsExpression) -> Unit) {
        visitor(receiver)
        arguments.forEach(visitor)
    }
}
