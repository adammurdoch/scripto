package net.rubygrapefruit.scripto.node

internal sealed interface JsExpression

internal class JsString(val value: String) : JsExpression

internal class JsVariableReference(val name: String) : JsExpression
