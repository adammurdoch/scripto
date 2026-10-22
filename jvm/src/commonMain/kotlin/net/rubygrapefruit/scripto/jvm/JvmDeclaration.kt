package net.rubygrapefruit.scripto.jvm

internal sealed interface JvmDeclaration {
    /**
     * Visits the declarations used directly by this declaration.
     */
    fun visitDeclarations(visitor: (JvmDeclaration) -> Unit) {
    }
}