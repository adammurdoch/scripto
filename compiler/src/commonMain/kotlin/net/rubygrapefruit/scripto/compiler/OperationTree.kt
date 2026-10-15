package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.parse.Position

internal sealed interface OperationTree

internal data class VariableDeclarationTree(val position: Position, val name: String, val value: ExpressionTree) : OperationTree
