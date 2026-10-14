package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.scripto.ir.VariableDeclaration
import java.lang.classfile.TypeKind

internal class JvmVariableDeclaration(
    val declaration: VariableDeclaration,
    val typeKind: TypeKind,
    val value: JvmExpression
): JvmOperation {
    val name: String
        get() = declaration.name
}
