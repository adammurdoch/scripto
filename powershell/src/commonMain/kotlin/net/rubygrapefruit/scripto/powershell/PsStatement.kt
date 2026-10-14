package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.ir.VariableDeclaration

internal sealed interface PsStatement {
    fun visitStatements(visitor: (PsStatement) -> Unit) {
    }
}

internal class PsCommandInvocation(val command: String, val args: List<PsExpression>) : PsStatement {
    override fun visitStatements(visitor: (PsStatement) -> Unit) {
        args.forEach { it.visitStatements(visitor) }
    }
}

internal class PsPipe(val left: PsCommandInvocation, val right: PsCommandInvocation) : PsStatement {
    override fun visitStatements(visitor: (PsStatement) -> Unit) {
        left.visitStatements(visitor)
        right.visitStatements(visitor)
    }
}

internal class PsIf(val condition: PsExpression, val statement: PsCommandInvocation): PsStatement

internal class PsVariableDeclaration(val declaration: VariableDeclaration, val value: PsExpression) : PsStatement {
    val name: String
        get() = declaration.name

    override fun visitStatements(visitor: (PsStatement) -> Unit) {
        value.visitStatements(visitor)
    }
}
