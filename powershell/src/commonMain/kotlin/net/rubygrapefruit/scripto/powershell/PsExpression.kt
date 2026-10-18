package net.rubygrapefruit.scripto.powershell

internal sealed interface PsExpression {
    fun visitStatements(visitor: (PsStatement) -> Unit) {
    }
}

internal class PsString(val value: String) : PsExpression

internal class PsInt(val value: Int) : PsExpression

internal data object PsTrue : PsExpression

internal data object PsFalse : PsExpression

internal class PsVariableReference(val name: String) : PsExpression

internal class PsIsNull(val value: PsExpression) : PsExpression

internal class PsTernaryExpression(val condition: PsExpression, val left: PsExpression, val right: PsExpression) : PsExpression {
    override fun visitStatements(visitor: (PsStatement) -> Unit) {
        condition.visitStatements(visitor)
        left.visitStatements(visitor)
        right.visitStatements(visitor)
    }
}

internal class PsInfixOperation(val left: PsExpression, val operator: String, val right: PsExpression) : PsExpression {
    override fun visitStatements(visitor: (PsStatement) -> Unit) {
        left.visitStatements(visitor)
        right.visitStatements(visitor)
    }
}

internal class PsFunctionCall(val receiver: PsExpression, val name: String) : PsExpression {
    override fun visitStatements(visitor: (PsStatement) -> Unit) {
        receiver.visitStatements(visitor)
    }
}

internal class PsCommandInvocationExpression(val intermediateVariable: String, val statements: List<PsStatement>) : PsExpression {
    override fun visitStatements(visitor: (PsStatement) -> Unit) {
        statements.forEach(visitor)
    }
}
