package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.generator.ExpressionWithDependencies

internal sealed interface BashExpression : ExpressionWithDependencies<BashStatement>

/**
 * A string that may need quoting or escaping.
 */
internal class BashString(val value: String) : BashExpression

/**
 * A raw string to include in the bash output.
 */
internal class BashWord(val value: String) : BashExpression

/**
 * A list of expressions concatenated together in the bash output.
 */
internal class BashConcatenatedExpressions(val values: List<BashExpression>) : BashExpression {
    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        for (expression in values) {
            expression.visitDependencies(visitor)
        }
    }
}

/**
 * An arithmetic expression.
 */
internal class BashArithmetic(val left: BashExpression, val operator: String, val right: BashExpression) : BashExpression {
    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        left.visitDependencies(visitor)
        right.visitDependencies(visitor)
    }
}

/**
 * A list of expressions
 */
internal class BashArrayLiteral(val elements: List<BashExpression>) : BashExpression {
    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        for (expression in elements) {
            expression.visitDependencies(visitor)
        }
    }
}

/**
 * An expression that requires a statement to be executed before it can be evaluated.
 */
internal class BashCommandInvocationExpression(val intermediateVariable: String, val statements: List<BashStatement>) : BashExpression {
    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        statements.forEach(visitor)
    }
}
