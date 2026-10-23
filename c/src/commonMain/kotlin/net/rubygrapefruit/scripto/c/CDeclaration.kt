package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.generator.Declaration

/**
 * Something that provides one or more declarations of types or functions.
 */
internal interface CDeclaration : Declaration<CDeclaration> {
    override fun visitOwnDeclarations(visitor: (CDeclaration) -> Unit) {
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
    override fun visitOwnDeclarations(visitor: (CDeclaration) -> Unit) {
        returnType?.visitDeclarations(visitor)
        for (parameter in parameters) {
            parameter.type.visitDeclarations(visitor)
        }
        for (statement in statements) {
            statement.visitOwnDeclarations(visitor)
            statement.visitExpressions { expression ->
                expression.visitOwnDeclarations(visitor)
            }
        }
    }
}