package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.generator.ExpressionWithDependencies

internal sealed interface BashStatement : ExpressionWithDependencies<BashStatement>

internal class BashCommandInvocation(
    val command: String,
    val args: List<BashExpression>,
    val redirectInput: BashExpression? = null
) : BashStatement {
    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        args.forEach { it.visitDependencies(visitor) }
        redirectInput?.visitDependencies(visitor)
    }
}

internal class BashIfStatement(val test: List<BashExpression>, val trueBranch: BashStatement, val falseBranch: BashStatement) : BashStatement {
    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        test.forEach { it.visitDependencies(visitor) }
    }
}

internal class BashVariableAssignment(val name: String, val value: BashExpression, val readOnly: Boolean) : BashStatement {
    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        value.visitDependencies(visitor)
    }
}
