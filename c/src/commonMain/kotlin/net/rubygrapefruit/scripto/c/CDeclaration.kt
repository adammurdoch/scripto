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

internal interface CFunctionDeclaration : CDeclaration {
    val name: String
}

internal class Parameter(val name: String, val type: CType)

internal class CGeneratedFunction(
    override val name: String,
    val returnType: CType?,
    val parameters: List<Parameter>,
    val statements: List<CStatement>
) : CFunctionDeclaration {
    override fun visitDeclarations(visitor: (CDeclaration) -> Unit) {
        returnType?.visitDeclarations(visitor)
        for (parameter in parameters) {
            parameter.type.visitDeclarations(visitor)
        }
        for (statement in statements) {
            statement.visitDeclarations(visitor)
        }
    }
}