package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.scripto.ir.Literal

internal sealed interface ExpressionTree

internal class VariableReferenceTree(val name: String) : ExpressionTree

internal class LiteralTree(val literal: Literal) : ExpressionTree

internal class InfixOperatorTree(val left: ExpressionTree, val operator: String, val right: ExpressionTree) : ExpressionTree
