package net.rubygrapefruit.scripto.jvm

/**
 * A static method.
 */
internal class JvmGeneratedMethod(
    val name: String,
    val descriptor: MethodDescriptor,
    val flags: MethodFlags,
    val operations: List<JvmOperation>
): JvmDeclaration