package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.generator.ExpressionWithDependencies
import net.rubygrapefruit.scripto.generator.TemporaryValue

internal sealed interface BashExpression : ExpressionWithDependencies<BashStatement> {
    fun visitDeclarations(visitor: (BashGeneratedFunction) -> Unit) {
    }

    fun visitVariables(visitor: (BashVariable) -> Unit) {
    }

    /**
     * Visits this expression plus any nested expressions. Expressions are visited in dependency order.
     */
    fun visitExpressions(visitor: (BashExpression) -> Unit) {
        visitor(this)
    }
}

/**
 * A string that may need quoting or escaping.
 */
internal class BashString(val value: String) : BashExpression

/**
 * A raw string to include in the bash output.
 */
internal class BashWord(val value: String) : BashExpression

internal sealed interface BashVariable {
    class Named(val name: String) : BashVariable

    class Temporary(override val nameHint: String) : BashVariable, TemporaryValue {
        override fun toString(): String {
            return nameHint
        }
    }
}

internal class BashVariableReference(val variable: BashVariable, val quoted: Boolean = true) : BashExpression {
    override fun visitVariables(visitor: (BashVariable) -> Unit) {
        visitor(variable)
    }
}

internal class BashVariableName(val variable: BashVariable) : BashExpression {
    override fun visitVariables(visitor: (BashVariable) -> Unit) {
        visitor(variable)
    }
}

internal sealed class BashCompositeExpression : BashExpression {
    override fun visitExpressions(visitor: (BashExpression) -> Unit) {
        visitChildren { expression ->
            expression.visitExpressions(visitor)
        }
        visitor(this)
    }

    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        visitChildren { expression ->
            expression.visitDependencies(visitor)
        }
    }

    protected abstract fun visitChildren(visitor: (BashExpression) -> Unit)
}

/**
 * A list of expressions concatenated together in the bash output.
 */
internal class BashConcatenatedExpressions(val values: List<BashExpression>) : BashCompositeExpression() {
    override fun visitChildren(visitor: (BashExpression) -> Unit) {
        values.forEach(visitor)
    }
}

/**
 * An arithmetic expression.
 */
internal class BashArithmetic(val left: BashExpression, val operator: String, val right: BashExpression) : BashCompositeExpression() {
    override fun visitChildren(visitor: (BashExpression) -> Unit) {
        visitor(left)
        visitor(right)
    }
}

/**
 * A list of expressions
 */
internal class BashArrayLiteral(val elements: List<BashExpression>) : BashCompositeExpression() {
    override fun visitChildren(visitor: (BashExpression) -> Unit) {
        elements.forEach(visitor)
    }
}

/**
 * An expression that requires a statement to be executed before it can be evaluated.
 */
internal class BashCommandInvocationExpression(val result: BashVariable, val statements: List<BashStatement>, val uses: BashGeneratedFunction? = null) : BashExpression {
    override fun visitDeclarations(visitor: (BashGeneratedFunction) -> Unit) {
        if (uses != null) {
            visitor(uses)
        }
    }

    override fun visitVariables(visitor: (BashVariable) -> Unit) {
        visitor(result)
    }

    override fun visitDependencies(visitor: (BashStatement) -> Unit) {
        statements.forEach(visitor)
    }

    override fun visitExpressions(visitor: (BashExpression) -> Unit) {
        if (result is BashExpression) {
            visitor(result)
        }
        visitor(this)
    }
}
