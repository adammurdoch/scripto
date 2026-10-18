package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.scripto.ir.Type
import net.rubygrapefruit.scripto.ir.VariableReference

internal sealed interface JvmExpression

internal class JvmString(val value: String) : JvmExpression

internal class JvmInt(val value: Int) : JvmExpression

internal data object JvmTrue : JvmExpression

internal data object JvmFalse : JvmExpression

internal class JvmVariableReference(val reference: VariableReference) : JvmExpression {
    val name: String
        get() = reference.target.name

    val type: Type
        get() = reference.type
}

internal class JvmIntAddition(val left: JvmExpression, val right: JvmExpression) : JvmExpression

internal class JvmStringEquality(val left: JvmExpression, val right: JvmExpression) : JvmExpression

internal class JvmIntEquality(val left: JvmExpression, val right: JvmExpression) : JvmExpression

internal class StaticMethodCall(
    val owner: TypeDescriptor.ObjectType,
    val name: String,
    val descriptor: MethodDescriptor,
    val args: List<JvmExpression>
) : JvmExpression

internal class VirtualMethodCall(
    val owner: TypeDescriptor.ObjectType,
    val name: String,
    val descriptor: MethodDescriptor,
    val receiver: JvmExpression,
    val args: List<JvmExpression>
) : JvmExpression

internal data object EmptyStringArray : JvmExpression
