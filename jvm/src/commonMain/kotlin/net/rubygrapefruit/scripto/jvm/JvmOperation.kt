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

internal class IfIntsEqual(val left: JvmExpression, val right: JvmExpression, val whenTrue: List<JvmOperation>) : JvmOperation {
    override fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        left.visitExpressions(visitor)
        right.visitExpressions(visitor)
        for (operation in whenTrue) {
            operation.visitExpressions(visitor)
        }
    }
}

internal class WhileIntLessThan(val left: JvmExpression, val right: JvmExpression, val loop: List<JvmOperation>) : JvmOperation {
    override fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        left.visitExpressions(visitor)
        right.visitExpressions(visitor)
        for (operation in loop) {
            operation.visitExpressions(visitor)
        }
    }
}

internal class JvmSetVariable(val name: String, val value: JvmExpression) : JvmOperation {
    override fun visitExpressions(visitor: (JvmExpression) -> Unit) {
        value.visitExpressions(visitor)
    }
}