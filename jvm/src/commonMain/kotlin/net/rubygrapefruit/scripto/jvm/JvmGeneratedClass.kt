package net.rubygrapefruit.scripto.jvm

internal class JvmGeneratedClass(
    val name: String,
    val descriptor: TypeDescriptor.ObjectType,
    val flags: ClassFlags,
    val interfaces: List<JvmGeneratedClass> = emptyList()
)