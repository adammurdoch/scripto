package net.rubygrapefruit.scripto.jvm

internal sealed interface JvmOperation {
    fun visitExpressions(visitor: (JvmExpression) -> Unit) {
    }
}

internal object ReturnVoid : JvmOperation

internal class ReturnValue(val type: TypeDescriptor, val value: JvmExpression) : JvmOperation {
    override fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        visitor(value)
    }
}
