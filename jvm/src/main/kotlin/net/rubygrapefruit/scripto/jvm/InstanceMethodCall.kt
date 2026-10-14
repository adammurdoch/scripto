package net.rubygrapefruit.scripto.jvm

import java.lang.constant.ClassDesc
import java.lang.constant.MethodTypeDesc

internal class InstanceMethodCall(
    val owner: ClassDesc,
    val name: String,
    val descriptor: MethodTypeDesc,
    val target: ReferenceExpression,
    val arguments: List<JvmExpression>
) : JvmOperation
