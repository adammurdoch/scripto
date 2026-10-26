package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.parse.Position
import net.rubygrapefruit.scripto.ir.*
import net.rubygrapefruit.strings.capitalized

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
        return resolveFunctionCall(operation, Resolved(library), variables, target, FunctionResolveRequest.OperationRequest)
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
                resolveFunctionCall(expression, Resolved(library), variables, target, FunctionResolveRequest.ExpressionRequest)
            }

            is ReceiverFunctionCallTree -> {
                val receiver = resolve(expression.receiver, variables, library, target)
                resolveFunctionCall(expression.target, receiver, variables, target, FunctionResolveRequest.ReceiverExpressionRequest)
            }
        }
    }

    private fun inferTypeParamFromArguments(argumentTrees: List<ExpressionTree>, arguments: List<Expression>): ResolveResult<Type> {
        return when (arguments.size) {
            0 -> Resolved(Type.Any)
            1 -> Resolved(arguments.first().type)
            else -> {
                val anyList = Type.List(Type.Any)
                val index = arguments.indexOfFirst { it.type != anyList }
                if (index < 0) {
                    Resolved(anyList)
                } else {
                    val type = arguments[index].type
                    val remaining = arguments.drop(index + 1)
                    if (remaining.all { it.type == type || it.type == anyList }) {
                        Resolved(type)
                    } else {
                        NotResolved.of(argumentTrees.first().position, "All arguments must have the same type, found: ${arguments.joinToString { it.type.toString() }}")
                    }
                }
            }
        }
    }

    private fun <S, T> resolveFunctionCall(
        function: FunctionCallTree,
        scope: ResolveResult<S>,
        variables: Map<String, VariableDeclaration>,
        target: TargetRuntime,
        request: FunctionResolveRequest<S, T>
    ): ResolveResult<T> {
        val arguments = resolveExpressions(function.arguments, variables, StandardLibrary, target)
        val definition = scope.mapping { scope ->
            locateFunction(function, request.functionsFor(scope)) { request.displayName(scope, function.name) }
        }
        val resolved = zipping(definition, arguments) { definition, arguments ->
            resolveTypeParameters(definition, function.arguments, arguments)
        }
        return zipping(scope, resolved, arguments) { scope, resolved, arguments ->
            target.inspect(resolved)
            request.create(function.position, scope, resolved, arguments)
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
        return locateFunction(operation, library) { "function ${operation.name}()" }
    }

    private fun locateFunction(operation: FunctionCallTree, scope: Functions, displayName: () -> String): ResolveResult<FunctionKind> {
        val definitions = scope.definitions(operation.name)
        if (definitions.isEmpty()) {
            return NotResolved.of(operation.position, "Unknown ${displayName()}")
        }
        val definition = selectFunction(definitions, operation.arguments.size)
        if (definition == null) {
            return NotResolved.of(operation.position, "Incorrect number of parameters for ${displayName()}")
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

    private sealed interface FunctionResolveRequest<S, T> {
        fun displayName(scope: S, functionName: String): String

        fun functionsFor(scope: S): Functions

        fun create(position: Position, scope: S, definition: FunctionDefinition, arguments: List<Expression>): ResolveResult<T>

        object OperationRequest : FunctionResolveRequest<Functions, UnitFunctionCall> {
            override fun displayName(scope: Functions, functionName: String): String {
                return "function $functionName()"
            }

            override fun functionsFor(scope: Functions): Functions {
                return scope
            }

            override fun create(position: Position, scope: Functions, definition: FunctionDefinition, arguments: List<Expression>): ResolveResult<UnitFunctionCall> {
                return if (definition !is UnitFunctionDefinition) {
                    NotResolved.of(position, "Return value ignored for ${definition.displayName}")
                } else {
                    Resolved(UnitFunctionCall(definition, arguments))
                }
            }
        }

        object ExpressionRequest : FunctionResolveRequest<Functions, FunctionCallExpression> {
            override fun displayName(scope: Functions, functionName: String): String {
                return "function $functionName()"
            }

            override fun functionsFor(scope: Functions): Functions {
                return scope
            }

            override fun create(position: Position, scope: Functions, definition: FunctionDefinition, arguments: List<Expression>): ResolveResult<FunctionCallExpression> {
                return if (definition !is TypedFunctionDefinition) {
                    NotResolved.of(position, "${definition.displayName.capitalized()} has no return value")
                } else {
                    Resolved(FunctionCallExpression(definition, arguments))
                }
            }
        }

        object ReceiverExpressionRequest : FunctionResolveRequest<Expression, ReceiverFunctionCallExpression> {
            override fun displayName(scope: Expression, functionName: String): String {
                return "function ${scope.type}.$functionName()"
            }

            override fun functionsFor(scope: Expression): Functions {
                return StandardLibrary.functionsFor(scope.type)
            }

            override fun create(position: Position, scope: Expression, definition: FunctionDefinition, arguments: List<Expression>): ResolveResult<ReceiverFunctionCallExpression> {
                return if (definition !is TypedFunctionDefinition) {
                    NotResolved.of(position, "${definition.displayName.capitalized()} has no return value")
                } else {
                    Resolved(ReceiverFunctionCallExpression(scope, definition, arguments))
                }
            }
        }
    }
}