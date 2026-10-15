package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.scripto.ir.*

internal class ProgramTree(val operations: List<OperationTree>) {
    fun resolve(library: StandardLibrary): ResolveResult<Program> {
        val variables = mutableMapOf<String, VariableDeclaration>()

        val resolvedOperations = operations.resolving { operation ->
            when (operation) {
                is FunctionCallTree -> {
                    resolveFunctionCall(operation, variables, library)
                }

                is VariableDeclarationTree -> {
                    val result = resolveVariableDeclaration(operation, variables, library)
                    if (result is Resolved) {
                        val declaration = result.result
                        variables[operation.name] = declaration
                    }
                    result
                }
            }
        }

        return resolvedOperations.mapping { operations ->
            Resolved(Program(operations))
        }
    }

    private fun resolveFunctionCall(
        operation: FunctionCallTree,
        variables: Map<String, VariableDeclaration>,
        library: StandardLibrary
    ): ResolveResult<FunctionCall> {
        val definition = locateFunction(operation, library)
        val arguments = resolveExpressions(operation.arguments, variables, library)
        return zipping(definition, arguments) { definition, arguments ->
            if (definition !is VoidFunctionDefinition) {
                NotResolved.of("Return value ignored for function '${operation.name}'")
            } else {
                Resolved(FunctionCall(definition, arguments))
            }
        }
    }

    private fun resolveVariableDeclaration(
        operation: VariableDeclarationTree,
        variables: Map<String, VariableDeclaration>,
        library: StandardLibrary
    ): ResolveResult<VariableDeclaration> {
        val initializer = resolve(operation.value, variables, library)
        return initializer.mapping { initializer ->
            if (variables.containsKey(operation.name)) {
                NotResolved.of(operation.position, "Variable '${operation.name}' declared multiple times")
            } else {
                Resolved(VariableDeclaration(operation.name, initializer))
            }
        }
    }

    private fun resolveExpressions(
        expressions: List<ExpressionTree>,
        variables: Map<String, VariableDeclaration>,
        library: StandardLibrary
    ): ResolveResult<List<Expression>> {
        return expressions.resolving { resolve(it, variables, library) }
    }

    private fun resolve(expression: ExpressionTree, variables: Map<String, VariableDeclaration>, library: StandardLibrary): ResolveResult<Expression> {
        return when (expression) {
            is LiteralTree -> Resolved(expression.literal)
            is VariableReferenceTree -> resolveVariableReference(variables, expression)
            is InfixOperatorTree -> resolveInfixOperator(library, expression, variables)

            is FunctionCallTree -> {
                val definition = locateFunction(expression, library)
                val arguments = resolveExpressions(expression.arguments, variables, library)
                zipping(definition, arguments) { definition, arguments ->
                    if (definition !is TypedFunctionDefinition) {
                        NotResolved.of(expression.position, "Function ${expression.name}() has no return value")
                    } else {
                        Resolved(FunctionCallExpression(definition, arguments))
                    }
                }
            }

            is ReceiverFunctionCallTree -> {
                val receiver = resolve(expression.receiver, variables, library)
                val arguments = resolveExpressions(expression.target.arguments, variables, library)
                val definition = receiver.mapping { receiver ->
                    locateFunction(receiver.type, expression.target, library)
                }
                zipping(receiver, definition, arguments) { receiver, definition, arguments ->
                    if (definition !is TypedFunctionDefinition) {
                        NotResolved.of(expression.target.position, "Function ${receiver.type}.${expression.target.name}() has no return value")
                    } else {
                        Resolved(ReceiverFunctionCallExpression(receiver, definition, arguments))
                    }
                }
            }
        }
    }

    private fun resolveVariableReference(
        variables: Map<String, VariableDeclaration>,
        expression: VariableReferenceTree
    ): ResolveResult<VariableReference> {
        val declaration = variables[expression.name]
        return if (declaration == null) {
            NotResolved.of(expression.position, "Unknown variable '${expression.name}'")
        } else {
            Resolved(VariableReference(declaration))
        }
    }

    private fun locateFunction(operation: FunctionCallTree, library: StandardLibrary): ResolveResult<FunctionDefinition> {
        val definitions = library.definitions(operation.name)
        if (definitions.isEmpty()) {
            return NotResolved.of(operation.position, "Unknown function ${operation.name}()")
        }
        val definition = definitions.find { it.args.size == operation.arguments.size }
        if (definition == null) {
            return NotResolved.of(operation.position, "Incorrect number of parameters for function ${operation.name}()")
        }
        return Resolved(definition)
    }

    private fun locateFunction(receiver: Type, operation: FunctionCallTree, library: StandardLibrary): ResolveResult<FunctionDefinition> {
        val definitions = library.definitions(receiver, operation.name)
        if (definitions.isEmpty()) {
            return NotResolved.of(operation.position,"Unknown function ${receiver}.${operation.name}()")
        }
        val definition = definitions.find { it.args.size == operation.arguments.size }
        if (definition == null) {
            return NotResolved.of(operation.position, "Incorrect number of parameters for function ${receiver}.${operation.name}()")
        }
        return Resolved(definition)
    }

    private fun resolveInfixOperator(
        library: StandardLibrary,
        expression: InfixOperatorTree,
        variables: Map<String, VariableDeclaration>
    ): ResolveResult<InfixFunctionCall> {
        val functions = library.operator(expression.operator)
        if (functions.isEmpty()) {
            return NotResolved.of("Unknown operator '${expression.operator}")
        }
        val left = resolve(expression.left, variables, library)
        val right = resolve(expression.right, variables, library)
        return zipping(left, right) { left, right ->
            val function = functions.find { left.type == it.args[0] && right.type == it.args[1] }
            if (function == null) {
                val definition = functions.first()
                val problems = mutableListOf<String>()
                if (left.type != definition.args[0]) {
                    problems.add("Argument is not of type ${definition.args[0]}")
                }
                if (right.type != definition.args[1]) {
                    problems.add("Argument is not of type ${definition.args[1]}")
                }
                NotResolved.of(problems)
            } else {
                Resolved(InfixFunctionCall(function, left, right))
            }
        }
    }
}