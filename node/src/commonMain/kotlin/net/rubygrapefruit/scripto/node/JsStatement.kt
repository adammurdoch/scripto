package net.rubygrapefruit.scripto.node

internal sealed interface JsStatement

internal class JsVariableDeclaration(val name: String, val value: JsExpression): JsStatement

internal class JsFunctionCall(val name: String, val arguments: List<JsExpression>) : JsStatement
