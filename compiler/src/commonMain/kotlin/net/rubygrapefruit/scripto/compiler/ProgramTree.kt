package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.scripto.ir.*

internal class ProgramTree(val operations: List<OperationTree>) {
    fun resolve(library: StandardLibrary, problems: MutableList<String>): Program {
        val variables = mutableMapOf<String, VariableDeclaration>()
        val resolvedOperations = mutableListOf<Operation>()
        for (operation in operations) {
            when (operation) {
                is FunctionCallTree -> {
                    val functionCall = resolveFunctionCall(operation, variables, library, problems)
                    if (functionCall != null) {
                        resolvedOperations.add(functionCall)
                    }
                }

                is VariableDeclarationTree -> {
                    val declaration = resolveVariableDeclaration(operation, variables, library, problems)
                    if (declaration != null) {
                        variables[operation.name] = declaration
                        resolvedOperations.add(declaration)
                    }
                }
            }
        }

        return Program(resolvedOperations)
    }

    private fun resolveFunctionCall(
        operation: FunctionCallTree,
        variables: Map<String, VariableDeclaration>,
        library: StandardLibrary,
        problems: MutableList<String>
    ): FunctionCall? {
        val definition = locateFunction(operation, library, problems) ?: return null
        if (definition !is VoidFunctionDefinition) {
            problems.add("Return value ignored for function '${operation.name}'")
            return null
        }
        val arguments = resolveExpressions(operation.arguments, variables, library, problems) ?: return null
        return FunctionCall(definition, arguments)
    }

    private fun resolveVariableDeclaration(
        operation: VariableDeclarationTree,
        variables: Map<String, VariableDeclaration>,
        library: StandardLibrary,
        problems: MutableList<String>
    ): VariableDeclaration? {
        if (variables.containsKey(operation.name)) {
            problems.add("Variable '${operation.name}' declared multiple times")
            return null
        }
        val initializer = resolve(operation.value, variables, library, problems) ?: return null
        return VariableDeclaration(operation.name, initializer)
    }

    private fun resolveExpressions(
        expressions: List<ExpressionTree>,
        variables: Map<String, VariableDeclaration>,
        library: StandardLibrary,
        problems: MutableList<String>
    ): List<Expression>? {
        val resolved = expressions.mapNotNull { resolve(it, variables, library, problems) }
        return if (resolved.size == expressions.size) {
            resolved
        } else {
            null
        }
    }

    private fun resolve(expression: ExpressionTree, variables: Map<String, VariableDeclaration>, library: StandardLibrary, problems: MutableList<String>): Expression? {
        return when (expression) {
            is LiteralTree -> expression.literal
            is VariableReferenceTree -> resolveVariableReference(variables, expression, problems)
            is InfixOperatorTree -> resolveInfixOperator(library, expression, problems, variables)
            is FunctionCallTree -> {
                val definition = locateFunction(expression, library, problems) ?: return null
                if (definition !is TypedFunctionDefinition) {
                    problems.add("Function '${expression.name}' has no return value")
                    return null
                }
                val arguments = resolveExpressions(expression.arguments, variables, library, problems) ?: return null
                FunctionCallExpression(definition, arguments)
            }
        }
    }

    private fun resolveVariableReference(
        variables: Map<String, VariableDeclaration>,
        expression: VariableReferenceTree,
        problems: MutableList<String>
    ): VariableReference? {
        val declaration = variables[expression.name]
        return if (declaration == null) {
            problems.add("Unknown variable '${expression.name}'")
            null
        } else {
            VariableReference(declaration)
        }
    }

    private fun locateFunction(operation: FunctionCallTree, library: StandardLibrary, problems: MutableList<String>): FunctionDefinition? {
        val definitions = library.definitions(operation.name)
        if (definitions.isEmpty()) {
            problems.add("Unknown function '${operation.name}'")
            return null
        }
        val definition = definitions.find { it.args.size == operation.arguments.size }
        if (definition == null) {
            problems.add("Incorrect number of parameters for function '${operation.name}'")
            return null
        }
        return definition
    }

    private fun resolveInfixOperator(
        library: StandardLibrary,
        expression: InfixOperatorTree,
        problems: MutableList<String>,
        variables: Map<String, VariableDeclaration>
    ): InfixFunctionCall? {
        val functions = library.operator(expression.operator)
        if (functions.isEmpty()) {
            problems.add("Unknown operator '${expression.operator}")
            return null
        }
        val left = resolve(expression.left, variables, library, problems)
        val right = resolve(expression.right, variables, library, problems)
        if (left == null || right == null) {
            return null
        }
        for (function in functions) {
            if (left.type == function.args[0] && right.type == function.args[1]) {
                return InfixFunctionCall(function, left, right)
            }
        }
        val definition = functions.first()
        if (left.type != definition.args[0]) {
            problems.add("Argument is not of type ${definition.args[0]}")
        }
        if (right.type != definition.args[1]) {
            problems.add("Argument is not of type ${definition.args[1]}")
        }
        return null
    }
}