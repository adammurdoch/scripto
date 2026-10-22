package net.rubygrapefruit.scripto.jvm

internal sealed interface JvmExpression {
    fun visitDeclarations(visitor: (JvmDeclaration) -> Unit) {
    }

    /**
     * Visits this expression plus any nested expressions
     */
    fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        visitor(this)
    }
}

internal class JvmString(val value: String) : JvmExpression

internal class JvmInt(val value: Int) : JvmExpression

internal data object JvmTrue : JvmExpression

internal data object JvmFalse : JvmExpression

internal class JvmVariableReference(val name: String, val type: TypeDescriptor) : JvmExpression

internal class GetStaticField(val owner: TypeDescriptor.ObjectType, val name: String, val type: TypeDescriptor) : JvmExpression

internal sealed class JvmCompositeExpression : JvmExpression {
    override fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        visitChildren(visitor)
        visitor(this)
    }

    protected abstract fun visitChildren(visitor: (JvmExpression) -> Unit)
}

internal class JvmIntAddition(val left: JvmExpression, val right: JvmExpression) : JvmCompositeExpression() {
    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        visitor(left)
        visitor(right)
    }
}

internal class JvmStringEquality(val left: JvmExpression, val right: JvmExpression) : JvmCompositeExpression() {
    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        visitor(left)
        visitor(right)
    }
}

internal class JvmIntEquality(val left: JvmExpression, val right: JvmExpression) : JvmCompositeExpression() {
    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        visitor(left)
        visitor(right)
    }
}

internal class ConstructorCall(val type: TypeDescriptor.ObjectType, val descriptor: MethodDescriptor, val args: List<JvmExpression>) : JvmCompositeExpression() {
    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        args.forEach(visitor)
    }
}

internal class StaticMethodCall(
    val owner: TypeDescriptor.ObjectType,
    val name: String,
    val descriptor: MethodDescriptor,
    val args: List<JvmExpression>,
    val declaredBy: JvmDeclaration? = null
) : JvmCompositeExpression() {
    override fun visitDeclarations(visitor: (JvmDeclaration) -> Unit) {
        if (declaredBy != null) {
            visitor(declaredBy)
        }
    }

    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        args.forEach(visitor)
    }
}

internal class VirtualMethodCall(
    val owner: TypeDescriptor.ObjectType,
    val name: String,
    val descriptor: MethodDescriptor,
    val receiver: JvmExpression,
    val args: List<JvmExpression>
) : JvmCompositeExpression() {
    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        args.forEach(visitor)
    }
}

internal data object EmptyStringArray : JvmExpression

internal class NewArray(val elementType: TypeDescriptor, val elements: List<JvmExpression>) : JvmCompositeExpression() {
    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        elements.forEach(visitor)
    }
}
