package net.rubygrapefruit.scripto.bash

internal sealed interface BashStatement

internal class BashCommandInvocation(
    val command: String,
    val args: List<BashExpression>,
    val redirectInput: BashExpression? = null
) : BashStatement {
    fun visitStatements(visitor: (BashStatement) -> Unit) {
        args.forEach { it.visitStatements(visitor) }
        redirectInput?.visitStatements(visitor)
    }
}

internal class BashIfStatement(val test: List<BashExpression>, val trueBranch: BashStatement, val falseBranch: BashStatement) : BashStatement {

}

internal class BashVariableAssignment(val name: String, val value: BashExpression) : BashStatement {
    fun visitStatements(visitor: (BashStatement) -> Unit) {
        value.visitStatements(visitor)
    }
}
