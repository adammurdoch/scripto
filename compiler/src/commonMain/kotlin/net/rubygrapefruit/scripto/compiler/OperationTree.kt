package net.rubygrapefruit.scripto.compiler

internal sealed interface OperationTree

internal data class VariableDeclarationTree(val name: String, val value: ExpressionTree) : OperationTree
