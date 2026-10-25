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
    // Can be empty, does not expose the parameters by name to the operations. Can still reference parameters by index
    val parameterNames: List<String>,
    val operations: List<JvmOperation>
) : JvmDeclaration {
    override fun visitOwnDeclarations(visitor: (JvmDeclaration) -> Unit) {
        for (operation in operations) {
            operation.visitExpressions { expression ->
                expression.visitDeclarations(visitor)
            }
        }
    }
}