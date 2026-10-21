package net.rubygrapefruit.scripto.c

/**
 * Something that provides one or more declarations of types or functions.
 */
internal interface CDeclaration {
    /**
     * Visits the declarations required by this declaration.
     */
    fun visitDeclarations(visitor: (CDeclaration) -> Unit) {
    }
}

internal class CGeneratedFunction(
    val name: String,
    val returnType: CType?,
    val parameters: List<CType>,
    val statements: List<CStatement>
) : CDeclaration