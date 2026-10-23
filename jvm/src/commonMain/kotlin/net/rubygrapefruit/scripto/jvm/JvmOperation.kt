package net.rubygrapefruit.scripto.jvm

internal sealed interface JvmOperation {
    fun visitExpressions(visitor: (JvmExpression) -> Unit) {
    }
}

internal object ReturnVoid : JvmOperation

internal class ReturnValue(val type: TypeDescriptor, val value: JvmExpression) : JvmOperation {
    override fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        visitor(value)
    }
}

internal sealed class JvmIf(val left: JvmExpression, val right: JvmExpression, val whenTrue: List<JvmOperation>) : JvmOperation {
    override fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        left.visitExpressions(visitor)
        right.visitExpressions(visitor)
        for (operation in whenTrue) {
            operation.visitExpressions(visitor)
        }
    }
}

internal class IfIntsEqual(left: JvmExpression, right: JvmExpression, whenTrue: List<JvmOperation>) : JvmIf(left, right, whenTrue)

internal class IfIntGreaterThan(left: JvmExpression, right: JvmExpression, whenTrue: List<JvmOperation>) : JvmIf(left, right, whenTrue)

internal class WhileIntLessThan(val left: JvmExpression, val right: JvmExpression, val loop: List<JvmOperation>) : JvmOperation {
    override fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        left.visitExpressions(visitor)
        right.visitExpressions(visitor)
        for (operation in loop) {
            operation.visitExpressions(visitor)
        }
    }
}

internal class JvmIncrementVariable(val name: String) : JvmOperation

internal class JvmDiscard(val value: JvmOperation) : JvmOperation {
    override fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        value.visitExpressions(visitor)
    }
}
