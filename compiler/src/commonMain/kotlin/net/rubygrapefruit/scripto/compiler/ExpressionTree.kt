package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.parse.Position
import net.rubygrapefruit.scripto.ir.Literal
import net.rubygrapefruit.scripto.ir.Operator

internal sealed interface ExpressionTree {
    /**
     * The location where this expression's value is produced.
     */
    val position: Position
}

internal class VariableReferenceTree(override val position: Position, val name: String) : ExpressionTree

internal class LiteralTree(override val position: Position, val literal: Literal) : ExpressionTree

internal class InfixOperatorTree(val left: ExpressionTree, val operatorPosition: Position, val operator: Operator, val right: ExpressionTree) : ExpressionTree {
    override val position: Position
        get() = operatorPosition
}

internal class ListLiteralTree(override val position: Position, val elements: List<ExpressionTree>) : ExpressionTree
