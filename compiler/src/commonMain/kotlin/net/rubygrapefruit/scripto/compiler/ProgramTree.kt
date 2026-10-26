package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.scripto.ir.*

internal class ProgramTree(val operations: List<OperationTree>) {
    fun resolve(library: StandardLibrary, target: TargetRuntime): ResolveResult<Program> {
        val variables = mutableMapOf<String, VariableDeclaration>()

        val resolvedOperations = operations.resolving { operation ->
            when (operation) {
                is FunctionCallTree -> {
                    resolveFunctionCall(operation, variables, library, target)
                }

                is VariableDeclarationTree -> {
                    val result = resolveVariableDeclaration(operation, variables, library, target)
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
        library: StandardLibrary,
        target: TargetRuntime
    ): ResolveResult<UnitFunctionCall> {
        val arguments = resolveExpressions(operation.arguments, variables, library, target)
        val definition = locateFunction(operation, library)
        val resolved = zipping(definition, arguments) { definition, arguments ->
            resolveTypeParameters(definition, operation.arguments, arguments)
        }
        return zipping(resolved, arguments) { resolved, arguments ->
            target.inspect(resolved)
            if (resolved !is UnitFunctionDefinition) {
                NotResolved.of(operation.position, "Return value ignored for function '${operation.name}'")
            } else {
                Resolved(UnitFunctionCall(resolved, arguments))
            }
        }
    }

    private fun resolveVariableDeclaration(
        operation: VariableDeclarationTree,
        variables: Map<String, VariableDeclaration>,
        library: StandardLibrary,
        target: TargetRuntime
    ): ResolveResult<VariableDeclaration> {
        val initializer = resolve(operation.value, variables, library, target)
        return initializer.mapping { initializer ->
            if (operation.name == StandardLibrary.argsVariable.name) {
                NotResolved.of(operation.position, "Variable '${operation.name}' hides main function parameters")
            } else if (variables.containsKey(operation.name)) {
                NotResolved.of(operation.position, "Variable '${operation.name}' declared multiple times")
            } else {
                Resolved(VariableDeclaration(operation.name, initializer))
            }
        }
    }

    private fun resolveExpressions(
        expressions: List<ExpressionTree>,
        variables: Map<String, VariableDeclaration>,
        library: StandardLibrary,
        target: TargetRuntime
    ): ResolveResult<List<Expression>> {
        return expressions.resolving { resolve(it, variables, library, target) }
    }

    private fun resolve(expression: ExpressionTree, variables: Map<String, VariableDeclaration>, library: StandardLibrary, target: TargetRuntime): ResolveResult<Expression> {
        return when (expression) {
            is LiteralTree -> Resolved(expression.literal)
            is VariableReferenceTree -> resolveVariableReference(variables, expression)
            is InfixOperatorTree -> resolveInfixOperator(library, expression, variables, target)
            is ListLiteralTree -> {
                val arguments = resolveExpressions(expression.elements, variables, library, target)
                val elementType = arguments.mapping { arguments ->
                    inferTypeParamFromArguments(expression.elements, arguments)
                }
                zipping(elementType, arguments) { elementType, arguments ->
                    val resolved = StandardLibrary.listConstructor.evaluate(elementType)
                    target.inspect(resolved)
                    Resolved(FunctionCallExpression(resolved, arguments))
                }
            }

            is FunctionCallTree -> {
                val arguments = resolveExpressions(expression.arguments, variables, library, target)
                val definition = locateFunction(expression, library)
                val resolved = zipping(definition, arguments) { definition, arguments ->
                    resolveTypeParameters(definition, expression.arguments, arguments)
                }
                zipping(resolved, arguments) { resolved, arguments ->
                    target.inspect(resolved)
                    if (resolved !is TypedFunctionDefinition) {
                        NotResolved.of(expression.position, "Function ${expression.name}() has no return value")
                    } else {
                        Resolved(FunctionCallExpression(resolved, arguments))
                    }
                }
            }

            is ReceiverFunctionCallTree -> {
                val receiver = resolve(expression.receiver, variables, library, target)
                val arguments = resolveExpressions(expression.target.arguments, variables, library, target)
                val definition = receiver.mapping { receiver ->
                    locateFunction(receiver.type, expression.target, library)
                }
                val resolved = zipping(definition, arguments) { definition, arguments ->
                    resolveTypeParameters(definition, expression.target.arguments, arguments)
                }
                zipping(receiver, resolved, arguments) { receiver, resolved, arguments ->
                    target.inspect(resolved)
                    if (resolved !is TypedFunctionDefinition) {
                        NotResolved.of(expression.target.position, "Function ${receiver.type}.${expression.target.name}() has no return value")
                    } else {
                        Resolved(ReceiverFunctionCallExpression(receiver, resolved, arguments))
                    }
                }
            }
        }
    }

    private fun inferTypeParamFromArguments(argumentTrees: List<ExpressionTree>, arguments: List<Expression>): ResolveResult<Type> {
        return if (arguments.isEmpty()) {
            Resolved(Type.Any)
        } else {
            val type = arguments.first().type
            if (arguments.all { it.type == type }) {
                Resolved(type)
            } else {
                NotResolved.of(argumentTrees.first().position, "All arguments must have the same type")
            }
        }
    }

    private fun resolveVariableReference(
        variables: Map<String, VariableDeclaration>,
        expression: VariableReferenceTree
    ): ResolveResult<VariableReference> {
        if (expression.name == StandardLibrary.argsVariable.name) {
            return Resolved(StandardLibrary.argsVariable)
        }
        val declaration = variables[expression.name]
        return if (declaration == null) {
            NotResolved.of(expression.position, "Unknown variable '${expression.name}'")
        } else {
            Resolved(VariableReference(declaration.type, declaration.name))
        }
    }

    private fun locateFunction(operation: FunctionCallTree, library: StandardLibrary): ResolveResult<FunctionKind> {
        val definitions = library.definitions(operation.name)
        if (definitions.isEmpty()) {
            return NotResolved.of(operation.position, "Unknown function ${operation.name}()")
        }
        val definition = selectFunction(definitions, operation.arguments.size)
        if (definition == null) {
            return NotResolved.of(operation.position, "Incorrect number of parameters for function ${operation.name}()")
        }
        return Resolved(definition)
    }

    private fun locateFunction(receiver: Type, operation: FunctionCallTree, library: StandardLibrary): ResolveResult<FunctionKind> {
        val definitions = library.definitions(receiver, operation.name)
        if (definitions.isEmpty()) {
            return NotResolved.of(operation.position, "Unknown function ${receiver}.${operation.name}()")
        }
        val definition = selectFunction(definitions, operation.arguments.size)
        if (definition == null) {
            return NotResolved.of(operation.position, "Incorrect number of parameters for function ${receiver}.${operation.name}()")
        }
        return Resolved(definition)
    }

    private fun selectFunction(candidates: List<FunctionKind>, argumentCount: Int): FunctionKind? {
        for (candidate in candidates) {
            if (argumentCount == candidate.parameterCount) {
                return candidate
            }
            if (candidate.isVarargs && argumentCount >= candidate.parameterCount - 1) {
                return candidate
            }
        }
        return null
    }

    private fun resolveTypeParameters(definition: FunctionKind, argumentTrees: List<ExpressionTree>, arguments: List<Expression>): ResolveResult<FunctionDefinition> {
        return when (definition) {
            is FunctionDefinition -> Resolved(definition)
            is NamedParameterizedFunctionDefinition -> inferTypeParamFromArguments(argumentTrees, arguments).mappingValue { definition.evaluate(it) }
        }
    }

    private fun resolveInfixOperator(
        library: StandardLibrary,
        expression: InfixOperatorTree,
        variables: Map<String, VariableDeclaration>,
        target: TargetRuntime
    ): ResolveResult<FunctionCallExpression> {
        val functions = library.operator(expression.operator)
        val left = resolve(expression.left, variables, library, target)
        val right = resolve(expression.right, variables, library, target)
        return zipping(left, right) { left, right ->
            val function = functions.find { left.type == it.parameters[0].type && right.type == it.parameters[1].type }
            if (function == null) {
                val definition = functions.first()
                val problems = mutableListOf<Problem>()
                if (left.type != definition.parameters[0].type) {
                    problems.add(Problem(expression.left.position, "Argument is not of type ${definition.parameters[0].type}"))
                }
                if (right.type != definition.parameters[1].type) {
                    problems.add(Problem(expression.right.position, "Argument is not of type ${definition.parameters[1].type}"))
                }
                NotResolved(problems)
            } else {
                Resolved(FunctionCallExpression(function, listOf(left, right)))
            }
        }
    }
}