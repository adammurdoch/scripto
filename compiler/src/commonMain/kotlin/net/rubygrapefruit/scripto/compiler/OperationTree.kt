package net.rubygrapefruit.scripto.compiler

internal sealed interface OperationTree

internal data class FunctionCallTree(val name: String, val arguments: List<ExpressionTree>) : OperationTree, ExpressionTree

internal data class ReceiverFunctionCallTree(val receiver: ExpressionTree, val target: FunctionCallTree) : OperationTree, ExpressionTree

internal data class VariableDeclarationTree(val name: String, val value: ExpressionTree) : OperationTree
