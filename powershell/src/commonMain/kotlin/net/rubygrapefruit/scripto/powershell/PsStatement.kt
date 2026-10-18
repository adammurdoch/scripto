package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.generator.ExpressionWithDependencies
import net.rubygrapefruit.scripto.ir.VariableDeclaration

internal sealed interface PsStatement : ExpressionWithDependencies<PsStatement>

internal class PsCommandInvocation(val command: String, val args: List<PsExpression>) : PsStatement {
    override fun visitDependencies(visitor: (PsStatement) -> Unit) {
        args.forEach { it.visitDependencies(visitor) }
    }
}

internal class PsPipe(val left: PsCommandInvocation, val right: PsCommandInvocation) : PsStatement {
    override fun visitDependencies(visitor: (PsStatement) -> Unit) {
        left.visitDependencies(visitor)
        right.visitDependencies(visitor)
    }
}

internal class PsIf(val condition: PsExpression, val statement: PsCommandInvocation) : PsStatement

internal class PsVariableDeclaration(val declaration: VariableDeclaration, val value: PsExpression) : PsStatement {
    val name: String
        get() = declaration.name

    override fun visitDependencies(visitor: (PsStatement) -> Unit) {
        value.visitDependencies(visitor)
    }
}
