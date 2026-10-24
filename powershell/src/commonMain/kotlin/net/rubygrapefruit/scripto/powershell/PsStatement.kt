package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.ir.VariableDeclaration

internal sealed interface PsStatement

internal class PsCommandInvocation(val command: String, val args: List<PsExpression>) : PsStatement, PsExpression

internal class PsPipe(val left: PsCommandInvocation, val right: PsCommandInvocation) : PsStatement

internal class PsIf(val condition: PsExpression, val statement: PsCommandInvocation) : PsStatement

internal class PsVariableDeclaration(val declaration: VariableDeclaration, val value: PsExpression) : PsStatement {
    val name: String
        get() = declaration.name
}
