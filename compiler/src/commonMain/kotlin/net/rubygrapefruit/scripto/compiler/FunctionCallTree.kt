package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.parse.Position

internal data class FunctionCallTree(override val position: Position, val name: String, val arguments: List<ExpressionTree>) : OperationTree, ExpressionTree

// No stdlib function that can be used as a statement yet, so not an [OperationTree]
internal data class MethodCallTree(val receiver: ExpressionTree, val target: FunctionCallTree) : ExpressionTree {
    override val position: Position
        get() = target.position
}
