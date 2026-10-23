package net.rubygrapefruit.scripto.jvm

/**
 * A static method.
 */
internal class JvmGeneratedMethod(
    val owner: JvmGeneratedType,
    val name: String,
    val descriptor: MethodDescriptor,
    val visibility: MethodVisibility,
    val isStatic: Boolean,
    val operations: List<JvmOperation>
) : JvmDeclaration {
    override fun visitDeclarations(visitor: (JvmDeclaration) -> Unit) {
        for (operation in operations) {
            operation.visitExpressions { expression ->
                expression.visitDeclarations(visitor)
            }
        }
    }
}