package net.rubygrapefruit.scripto.c

internal class CFunctionCall(
    val name: String,
    val arguments: List<CExpression>,
    val declaredBy: CDeclaration? = null
) : CCompositeExpression(), CStatement {
    constructor(function: FunctionSnippet, arguments: List<CExpression>) : this(function.name, arguments, function)

    override fun visitOwnDeclarations(visitor: (CDeclaration) -> Unit) {
        if (declaredBy != null) {
            visitor(declaredBy)
        }
    }

    override fun visitChildren(visitor: (CExpression) -> Unit) {
        arguments.forEach(visitor)
    }
}
