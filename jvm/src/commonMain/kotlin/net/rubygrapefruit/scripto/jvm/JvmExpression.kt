package net.rubygrapefruit.scripto.jvm

internal sealed interface JvmExpression {
    fun visitDeclarations(visitor: (JvmDeclaration) -> Unit) {
    }

    /**
     * Visits this expression plus any nested expressions. Expressions are visited in dependency order.
     */
    fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        visitor(this)
    }
}

internal class JvmString(val value: String) : JvmExpression

internal class JvmInt(val value: Int) : JvmExpression

internal data object JvmNull : JvmExpression

internal data object JvmTrue : JvmExpression

internal data object JvmFalse : JvmExpression

internal class JvmVariableReference(val name: String, val type: TypeDescriptor) : JvmExpression

internal class JvmParameterReference(val index: Int, val type: TypeDescriptor) : JvmExpression

internal class JvmCast(val type: TypeDescriptor.ReferenceType, val value: JvmExpression) : JvmCompositeExpression() {
    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        visitor(value)
    }
}

internal class GetStaticField(val owner: TypeDescriptor.ClassType, val name: String, val type: TypeDescriptor) : JvmExpression

internal sealed class JvmCompositeExpression : JvmExpression {
    override fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        visitChildren { expression ->
            expression.visitExpressions(visitor)
        }
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

internal class JvmIntEquality(val left: JvmExpression, val right: JvmExpression) : JvmCompositeExpression() {
    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        visitor(left)
        visitor(right)
    }
}

internal class ConstructorCall(
    val type: TypeDescriptor.ClassType,
    val descriptor: MethodDescriptor,
    val arguments: List<JvmExpression>,
    val declaredBy: JvmDeclaration? = null
) : JvmCompositeExpression() {

    override fun visitDeclarations(visitor: (JvmDeclaration) -> Unit) {
        if (declaredBy != null) {
            visitor(declaredBy)
        }
    }

    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        arguments.forEach(visitor)
    }
}

internal class StaticMethodCall(
    val owner: TypeDescriptor.ObjectType,
    val name: String,
    val descriptor: MethodDescriptor,
    val arguments: List<JvmExpression>,
    val declaredBy: JvmDeclaration? = null
) : JvmCompositeExpression() {
    constructor(method: JvmGeneratedMethod, args: List<JvmExpression>) : this(method.owner.descriptor, method.name, method.descriptor, args, method)

    override fun visitDeclarations(visitor: (JvmDeclaration) -> Unit) {
        if (declaredBy != null) {
            visitor(declaredBy)
        }
    }

    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        arguments.forEach(visitor)
    }
}

internal class VirtualMethodCall(
    val owner: TypeDescriptor.ObjectType,
    val name: String,
    val descriptor: MethodDescriptor,
    val receiver: JvmExpression,
    val arguments: List<JvmExpression>,
    val declaredBy: JvmDeclaration? = null
) : JvmCompositeExpression(), JvmOperation {
    constructor(method: JvmGeneratedMethod, receiver: JvmExpression, arguments: List<JvmExpression>) : this(
        method.owner.descriptor,
        method.name,
        method.descriptor,
        receiver,
        arguments,
        method
    )

    override fun visitDeclarations(visitor: (JvmDeclaration) -> Unit) {
        if (declaredBy != null) {
            visitor(declaredBy)
        }
    }

    override fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        super<JvmCompositeExpression>.visitExpressions(visitor)
    }

    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        visitor(receiver)
        arguments.forEach(visitor)
    }
}

internal data object EmptyStringArray : JvmExpression

internal class JvmArrayLength(val value: JvmExpression) : JvmCompositeExpression() {
    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        visitor(value)
    }
}

internal class JvmGetArrayElement(val elementType: TypeDescriptor.ClassType, val value: JvmExpression, val index: JvmExpression) : JvmCompositeExpression() {
    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        visitor(value)
        visitor(index)
    }
}

internal class NewArray(val elementType: TypeDescriptor, val elements: List<JvmExpression>) : JvmCompositeExpression() {
    override fun visitChildren(visitor: (JvmExpression) -> Unit) {
        elements.forEach(visitor)
    }
}
