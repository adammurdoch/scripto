package net.rubygrapefruit.scripto.node

internal sealed interface JsExpression

internal class JsString(val value: String) : JsExpression

internal class JsInt(val value: Int) : JsExpression

internal class JsBoolean(val value: Boolean) : JsExpression

internal class JsVariableReference(val name: String) : JsExpression

internal class JsInfix(val left: JsExpression, val operator: String, val right: JsExpression) : JsExpression

internal class JsArrayLiteral(val elements: List<JsExpression>) : JsExpression
