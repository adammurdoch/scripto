package net.rubygrapefruit.scripto.jvm

internal class JvmProgram(val operations: List<JvmOperation>) {
    val variables: List<JvmVariableDeclaration>
        get() = operations.filterIsInstance<JvmVariableDeclaration>()
}
