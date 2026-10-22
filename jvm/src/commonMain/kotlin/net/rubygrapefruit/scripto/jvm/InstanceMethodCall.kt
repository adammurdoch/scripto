package net.rubygrapefruit.scripto.jvm

internal class InstanceMethodCall(
    val owner: TypeDescriptor.ObjectType,
    val name: String,
    val descriptor: MethodDescriptor,
    val receiver: JvmExpression,
    val arguments: List<JvmExpression>
) : JvmOperation {
    override fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        receiver.visitExpressions(visitor)
        for (argument in arguments) {
            argument.visitExpressions(visitor)
        }
    }
}
