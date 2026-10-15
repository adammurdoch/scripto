package net.rubygrapefruit.scripto.compiler

internal data class FunctionCallTree(val name: String, val arguments: List<ExpressionTree>) : OperationTree, ExpressionTree

// No stdlib function that can be used as a statement yet
internal data class ReceiverFunctionCallTree(val receiver: ExpressionTree, val target: FunctionCallTree) : ExpressionTree
