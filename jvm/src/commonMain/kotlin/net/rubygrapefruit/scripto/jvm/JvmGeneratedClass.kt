package net.rubygrapefruit.scripto.jvm

internal class JvmGeneratedClass(
    val descriptor: TypeDescriptor.ObjectType,
    val flags: ClassFlags,
    val methods: List<JvmGeneratedMethod>
) : JvmDeclaration