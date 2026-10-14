package net.rubygrapefruit.scripto.bash

internal sealed interface BashExpression {
    fun visitStatements(visitor: (BashStatement) -> Unit) {
    }
}

/**
 * A string that may need quoting or escaping.
 */
internal class BashString(val value: String) : BashExpression

/**
 * A literal string to include in the bash output.
 */
internal class BashWord(val value: String) : BashExpression

/**
 * A list of expressions concatenated together in the bash output.
 */
internal class BashConcatenatedExpressions(val values: List<BashExpression>) : BashExpression

/**
 * An arithmetic expression.
 */
internal class BashArithmetic(val left: BashExpression, val operator: String, val right: BashExpression) : BashExpression

/**
 * An expression that requires a statement to be executed before it can be evaluated.
 */
internal class BashCommandInvocationExpression(val intermediateVariable: String, val statement: BashStatement) : BashExpression {
    override fun visitStatements(visitor: (BashStatement) -> Unit) {
        visitor(statement)
    }
}
