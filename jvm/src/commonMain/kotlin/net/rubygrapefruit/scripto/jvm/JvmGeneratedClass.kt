package net.rubygrapefruit.scripto.jvm

internal sealed interface JvmGeneratedType {
    val name: String
    val descriptor: TypeDescriptor.ObjectType
    val visibility: ClassVisibility
    val interfaces: List<JvmGeneratedInterface>
}

internal class JvmGeneratedClass(
    override val name: String,
    override val descriptor: TypeDescriptor.ClassType,
    override val visibility: ClassVisibility,
    val isFinal: Boolean,
    override val interfaces: List<JvmGeneratedInterface> = emptyList()
) : JvmGeneratedType

internal class JvmGeneratedInterface(
    override val name: String,
    override val descriptor: TypeDescriptor.InterfaceType,
    override val visibility: ClassVisibility
) : JvmGeneratedType {
    override val interfaces: List<JvmGeneratedInterface>
        get() = emptyList()
}