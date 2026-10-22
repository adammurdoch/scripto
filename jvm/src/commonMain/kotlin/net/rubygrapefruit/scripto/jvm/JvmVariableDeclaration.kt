package net.rubygrapefruit.scripto.jvm

internal class JvmVariableDeclaration(
    val name: String,
    val type: TypeDescriptor,
    val value: JvmExpression
) : JvmOperation {
    override fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        value.visitExpressions(visitor)
    }
}
