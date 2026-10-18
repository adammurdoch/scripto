package net.rubygrapefruit.scripto.jvm

internal class InstanceMethodCall(
    val owner: TypeDescriptor.ObjectType,
    val name: String,
    val descriptor: MethodDescriptor,
    val target: ReferenceExpression,
    val arguments: List<JvmExpression>
) : JvmOperation
