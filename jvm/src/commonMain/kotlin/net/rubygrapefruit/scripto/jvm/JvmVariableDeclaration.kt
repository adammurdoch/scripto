package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.scripto.ir.VariableDeclaration

internal class JvmVariableDeclaration(
    val declaration: VariableDeclaration,
    val type: TypeDescriptor,
    val value: JvmExpression
): JvmOperation {
    val name: String
        get() = declaration.name
}
