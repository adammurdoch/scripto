package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.file.PosixPermissions
import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.*
import net.rubygrapefruit.scripto.ir.*

class BashGenerator {
    fun generate(program: Program, outputFile: RegularFile) {
        val scope = StatementScope()

        val functionMapper = createFunctionMapper(scope)
        val operationMapper = createOperationMapper(scope, functionMapper)

        val statements = operationMapper.map(program.operations)
        val bashProgram = BashProgram(statements)

        writeTo(outputFile) {
            appendln("# generated file")
            appendln()
            for (statement in bashProgram.statements) {
                append(statement)
            }
        }
        outputFile.setPermissions(PosixPermissions.readWriteFile.executable())
    }

    private fun createOperationMapper(scope: StatementScope, functionMapper: FunctionMapper<BashExpression>): OperationMapper<BashStatement> {
        return OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                BashCommandInvocation("echo", emptyList())
            }
            map(StandardLibrary.printlnOneArg) { call ->
                val call = StandardLibrary.convertPrintln(call)
                val args = call.arguments.map { toExpression(it, scope, functionMapper) }
                BashCommandInvocation("echo", args)
            }
            map { declaration ->
                val value = declaration.value
                BashVariableAssignment(declaration.name, toExpression(value, scope, functionMapper), readOnly = true)
            }
            collectDependencies { statement: BashStatement ->
                addDependencies(statement)
            }
        }
    }

    private fun createFunctionMapper(scope: StatementScope): FunctionMapper<BashExpression> {
        return FunctionMapper.of {
            map(StandardLibrary.fileConstructor) { expression, functionMapper ->
                toExpression(expression.args.first(), scope, functionMapper)
            }
            map(StandardLibrary.listConstructor) { expression, functionMapper ->
                BashArrayLiteral(expression.args.map { toExpression(it, scope, functionMapper) })
            }
            mapOperator(StandardLibrary.intAddition, scope) { left, right ->
                BashArithmetic(left, "+", right)
            }
            mapOperator(StandardLibrary.stringConcatenation, scope) { left, right ->
                BashConcatenatedExpressions(listOf(left, right))
            }
            val equality = { left: BashExpression, right: BashExpression ->
                val test = listOf(left, BashWord("="), right)
                val trueBranch = BashVariableAssignment("TMP_VAR", BashWord("true"), readOnly = false)
                val falseBranch = BashVariableAssignment("TMP_VAR", BashWord("false"), readOnly = false)
                val statement = BashIfStatement(test, trueBranch, falseBranch)
                BashCommandInvocationExpression("TMP_VAR", statement)
            }
            mapOperator(StandardLibrary.stringEquality, scope, equality)
            mapOperator(StandardLibrary.intEquality, scope, equality)
            mapOperator(StandardLibrary.booleanEquality, scope, equality)
        }
    }

    private fun FunctionMapper.Builder<BashExpression>.mapOperator(
        operator: OperatorDefinition,
        scope: StatementScope,
        builder: (BashExpression, BashExpression) -> BashExpression
    ) {
        map(operator) { expression, functionMapper ->
            val left = toExpression(expression.args[0], scope, functionMapper)
            val right = toExpression(expression.args[1], scope, functionMapper)
            builder(left, right)
        }
    }

    private fun toExpression(expression: Expression, scope: StatementScope, functionMapper: FunctionMapper<BashExpression>): BashExpression {
        return when (expression) {
            is StringLiteral -> BashString(expression.value)
            is IntLiteral -> BashWord(expression.value.toString())
            is BooleanLiteral -> BashWord(expression.value.toString())
            is VariableReference -> varReferenceToBash(expression)
            is FunctionCallExpression -> functionMapper.map(expression)
            is ReceiverFunctionCallExpression -> {
                if (expression.target == StringFunctions.toString) {
                    toExpression(expression.receiver, scope, functionMapper)
                } else if (expression.target == IntFunctions.toString) {
                    toExpression(expression.receiver, scope, functionMapper)
                } else if (expression.target == BooleanFunctions.toString) {
                    toExpression(expression.receiver, scope, functionMapper)
                } else if (expression.target == FileFunctions.toString) {
                    BashConcatenatedExpressions(listOf(BashString("File("), toExpression(expression.receiver, scope, functionMapper), BashString(")")))
                } else if (expression.target == ListFunctions.toString) {
                    val receiver = expression.receiver
                    if (receiver is VariableReference) {
                        BashWord("\${${receiver.target.name}[@]}")
                    } else {
                        TODO()
                    }
                } else if (expression.target == FileFunctions.readText) {
                    val varName = scope.newVarName("READ_TEXT")
                    val readText = readFileIntoVar(toExpression(expression.receiver, scope, functionMapper), varName)
                    BashCommandInvocationExpression(varName, readText)
                } else {
                    throw IllegalArgumentException("Unknown ${expression.target.displayName} for ${expression.receiver.type}")
                }
            }
        }
    }

    private fun varReferenceToBash(expression: VariableReference): BashWord {
        return when (expression.type) {
            Type.String, Type.File -> BashWord("\"\$${expression.target.name}\"")
            else -> BashWord("\$${expression.target.name}")
        }
    }

    private fun readFileIntoVar(file: BashExpression, varName: String): BashCommandInvocation {
        return BashCommandInvocation("IFS= read -rd '' $varName", emptyList(), redirectInput = file)
    }

    private fun SourceGenerator.append(statement: BashStatement) {
        when (statement) {
            is BashVariableAssignment -> append(statement)
            is BashCommandInvocation -> append(statement)
            is BashIfStatement -> append(statement)
        }
    }

    private fun SourceGenerator.append(declaration: BashVariableAssignment) {
        if (declaration.readOnly) {
            append("readonly ")
        }
        append(declaration.name)
        append("=")
        append(declaration.value)
        appendln()
    }

    private fun SourceGenerator.append(statement: BashCommandInvocation) {
        append(statement.command)
        for (arg in statement.args) {
            append(" ")
            append(arg)
        }
        if (statement.redirectInput != null) {
            append("<")
            append(statement.redirectInput)
        }
        appendln()
    }

    private fun SourceGenerator.append(statement: BashIfStatement) {
        append("if [ ")
        for (arg in statement.test) {
            append(arg)
            append(' ')
        }
        appendln("]; then")
        append(statement.trueBranch)
        appendln("else")
        append(statement.falseBranch)
        appendln("fi")
    }

    private fun SourceGenerator.append(expression: BashExpression) {
        when (expression) {
            is BashString -> {
                append("\"")
                for (ch in expression.value) {
                    when (ch) {
                        '`', '$', '\\' -> {
                            append("\\")
                            append(ch)
                        }

                        else -> append(ch)
                    }
                }
                append("\"")
            }

            is BashWord -> append(expression.value)

            is BashConcatenatedExpressions -> {
                for (nested in expression.values) {
                    append(nested)
                }
            }

            is BashArithmetic -> {
                append("$((")
                append(expression.left)
                append(expression.operator)
                append(expression.right)
                append("))")
            }

            is BashArrayLiteral -> {
                append("(")
                expression.elements.forEachIndexed { index, element ->
                    if (index > 0) {
                        append(" ")
                    }
                    append(element)
                }
                append(")")
            }

            is BashCommandInvocationExpression -> {
                append("\"$")
                append(expression.intermediateVariable)
                append("\"")
            }
        }
    }
}