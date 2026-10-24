package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.generator.ExpressionWithDependencies

internal sealed interface BashStatement : ExpressionWithDependencies<BashStatement> {
    /**
     * Visit any expressions used by this statement.
     */
    fun visitExpressions(visitor: (BashExpression) -> Unit) {
    }
}

internal class BashCommandInvocation(
    val command: String,
    val arguments: List<BashExpression>,
    val redirectInput: BashExpression? = null
) : BashStatement {
    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        arguments.forEach { it.visitDependencies(visitor) }
        redirectInput?.visitDependencies(visitor)
    }

    override fun visitExpressions(visitor: (BashExpression) -> Unit) {
        for (argument in arguments) {
            argument.visitExpressions(visitor)
        }
        redirectInput?.visitExpressions(visitor)
    }
}

internal class BashIfStatement(val test: List<BashExpression>, val trueBranch: BashStatement, val falseBranch: BashStatement) : BashStatement {
    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        test.forEach { it.visitDependencies(visitor) }
    }

    override fun visitExpressions(visitor: (BashExpression) -> Unit) {
        for (expression in test) {
            expression.visitExpressions(visitor)
        }
        trueBranch.visitExpressions(visitor)
        falseBranch.visitExpressions(visitor)
    }
}

internal class BashVariableAssignment(val target: BashVariable, val value: BashExpression) : BashStatement {
    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        value.visitDependencies(visitor)
    }

    override fun visitExpressions(visitor: (BashExpression) -> Unit) {
        value.visitExpressions(visitor)
    }
}

internal class BashVariableValueAppend(val name: String, val value: BashExpression) : BashStatement {
    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        value.visitDependencies(visitor)
    }

    override fun visitExpressions(visitor: (BashExpression) -> Unit) {
        value.visitExpressions(visitor)
    }
}

internal class BashForLoop(val elementVarName: String, val elements: BashExpression, val statements: List<BashStatement>) : BashStatement {
    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        elements.visitDependencies(visitor)
    }

    override fun visitExpressions(visitor: (BashExpression) -> Unit) {
        elements.visitExpressions(visitor)
        for (statement in statements) {
            statement.visitExpressions(visitor)
        }
    }
}
