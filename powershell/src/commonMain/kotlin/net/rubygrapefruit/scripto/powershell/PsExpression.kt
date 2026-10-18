package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.generator.ExpressionWithDependencies

internal sealed interface PsExpression : ExpressionWithDependencies<PsStatement>

internal class PsString(val value: String) : PsExpression

internal class PsInt(val value: Int) : PsExpression

internal data object PsTrue : PsExpression

internal data object PsFalse : PsExpression

internal class PsVariableReference(val name: String) : PsExpression

internal class PsIsNull(val value: PsExpression) : PsExpression

internal class PsTernaryExpression(val condition: PsExpression, val left: PsExpression, val right: PsExpression) : PsExpression {
    override fun visitDependencies(visitor: (PsStatement) -> Unit) {
        condition.visitDependencies(visitor)
        left.visitDependencies(visitor)
        right.visitDependencies(visitor)
    }
}

internal class PsInfixOperation(val left: PsExpression, val operator: String, val right: PsExpression) : PsExpression {
    override fun visitDependencies(visitor: (PsStatement) -> Unit) {
        left.visitDependencies(visitor)
        right.visitDependencies(visitor)
    }
}

internal class PsFunctionCall(val receiver: PsExpression, val name: String) : PsExpression {
    override fun visitDependencies(visitor: (PsStatement) -> Unit) {
        receiver.visitDependencies(visitor)
    }
}

internal class PsCommandInvocationExpression(val intermediateVariable: String, val statements: List<PsStatement>) : PsExpression {
    override fun visitDependencies(visitor: (PsStatement) -> Unit) {
        statements.forEach(visitor)
    }
}
