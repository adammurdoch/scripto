package net.rubygrapefruit.scripto.c

internal class CFunctionCall(
    val name: String,
    val arguments: List<CExpression>,
    val declaredBy: ExternalDeclaration? = null
) : CStatement, CExpression {
    constructor(function: FunctionSnippet, arguments: List<CExpression>) : this(function.name, arguments, function)

    override fun visitDeclarations(visitor: (ExternalDeclaration) -> Unit) {
        if (declaredBy != null) {
            visitor(declaredBy)
        }
        arguments.forEach { it.visitDeclarations(visitor) }
    }

    override fun visitDependencies(visitor: (CStatement) -> Unit) {
        arguments.forEach { it.visitDependencies(visitor) }
    }
}
