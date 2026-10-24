package net.rubygrapefruit.scripto.powershell

internal sealed interface PsExpression

internal class PsString(val value: String) : PsExpression

internal class PsInt(val value: Int) : PsExpression

internal data object PsTrue : PsExpression

internal data object PsFalse : PsExpression

internal class PsRaw(val value: String) : PsExpression

internal class PsVariableReference(val name: String) : PsExpression

internal class PsIsNull(val value: PsExpression) : PsExpression

internal class PsTernaryExpression(val condition: PsExpression, val left: PsExpression, val right: PsExpression) : PsExpression

internal class PsInfixOperation(val left: PsExpression, val operator: String, val right: PsExpression) : PsExpression

internal class PsFunctionCall(val receiver: PsExpression, val name: String) : PsExpression

internal class PsArrayOperator(val elements: List<PsExpression>) : PsExpression

internal class PsDotNetFunctionInvocation(val qualifiedName: String, val arguments: List<PsExpression>) : PsExpression
