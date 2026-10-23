package net.rubygrapefruit.scripto.jvm

/**
 * A static method.
 */
internal class JvmGeneratedMethod(
    val owner: JvmGeneratedClass,
    val name: String,
    val descriptor: MethodDescriptor,
    val flags: MethodFlags,
    val operations: List<JvmOperation>
) : JvmDeclaration {
    fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        for (operation in operations) {
            operation.visitExpressions(visitor)
        }
    }
}