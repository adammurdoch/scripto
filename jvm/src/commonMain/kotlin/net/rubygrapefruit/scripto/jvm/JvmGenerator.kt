package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.FunctionMapper
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.ir.*

class JvmGenerator {
    private val integer = TypeDescriptor.ofClass("java.lang.Integer")
    private val boolean = TypeDescriptor.ofClass("java.lang.Boolean")
    private val files = TypeDescriptor.ofClass("java.nio.file.Files")
    private val path = TypeDescriptor.ofInterface("java.nio.file.Path")
    private val arrays = TypeDescriptor.ofClass("java.util.Arrays")
    private val objectArray = TypeDescriptor.Object.arrayType()
    private val printStream = TypeDescriptor.ofClass("java.io.PrintStream")
    private val system = TypeDescriptor.ofClass("java.lang.System")
    private val stringBuilder = TypeDescriptor.ofClass("java.lang.StringBuilder")

    private val voidMethod = MethodDescriptor.ofVoid()
    private val toStringMethod = MethodDescriptor.of(TypeDescriptor.String)
    private val stringToVoidMethod = MethodDescriptor.ofVoid(TypeDescriptor.String)
    private val stringToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.String)
    private val stringToPathMethod = MethodDescriptor.of(path, TypeDescriptor.String, TypeDescriptor.String.arrayType())
    private val intToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Int)
    private val boolToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Boolean)
    private val objectToBoolMethod = MethodDescriptor.of(TypeDescriptor.Boolean, TypeDescriptor.Object)
    private val pathToStringMethod = MethodDescriptor.of(TypeDescriptor.String, path)
    private val objectArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, objectArray)
    private val intArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Int.arrayType())
    private val booleanArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Boolean.arrayType())

    private val systemOut = GetStaticField(system, "out", printStream)

    private val thisClass = TypeDescriptor.ofClass("App")
    private val listToString by lazy {
        val builder = JvmVariableReference("builder", TypeDescriptor.String)
        val stringToStringBuilder = MethodDescriptor.of(stringBuilder, TypeDescriptor.String)
        JvmGeneratedMethod(
            "\$listToString", objectArrayToStringMethod, MethodFlags.PrivateStatic, listOf(
                JvmVariableDeclaration("builder", stringBuilder, ConstructorCall(stringBuilder, voidMethod, emptyList())),
                InstanceMethodCall(stringBuilder, "append", stringToStringBuilder, builder, listOf(JvmString("["))),
                InstanceMethodCall(stringBuilder, "append", stringToStringBuilder, builder, listOf(JvmString("]"))),
                ReturnValue(
                    TypeDescriptor.String,
                    VirtualMethodCall(stringBuilder, "toString", toStringMethod, builder, emptyList())
                )
            )
        )
    }

    fun generate(program: Program, output: RegularFile): GeneratedClass {
        ClassFileWriter().writeTo(thisClass, output) { generate(program) }
        return GeneratedClass("App")
    }

    private fun ClassBuilder.generate(program: Program) {
        val functionMapper = createFunctionMapper()
        val operationMapper = createOperationMapper(functionMapper)

        val operations = mutableListOf<JvmOperation>()
        operationMapper.mapInto(program.operations, operations)
        operations.add(ReturnVoid)

        val stringArray = TypeDescriptor.String.arrayType()

        val seen = mutableSetOf<JvmGeneratedMethod>()

        val mainMethod = JvmGeneratedMethod("main", MethodDescriptor.ofVoid(stringArray), MethodFlags.PublicStatic, operations)
        mainMethod.visitExpressions { expression ->
            expression.visitDeclarations { declaration ->
                if (declaration is JvmGeneratedMethod && seen.add(declaration)) {
                    generate(declaration)
                }
            }
        }

        generate(mainMethod)
    }

    private fun createOperationMapper(functionMapper: FunctionMapper<JvmExpression>): OperationMapper<JvmOperation> {
        return OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                InstanceMethodCall(printStream, "println", voidMethod, systemOut, emptyList())
            }
            map(StandardLibrary.printlnOneArg) { call ->
                val call = StandardLibrary.convertPrintln(call)
                val args = call.arguments.map { toExpression(it, functionMapper) }
                InstanceMethodCall(printStream, "println", stringToVoidMethod, systemOut, args)
            }
            map { declaration ->
                val type = toTypeDescriptor(declaration.type)
                JvmVariableDeclaration(declaration.name, type, toExpression(declaration.value, functionMapper))
            }
        }
    }

    private fun createFunctionMapper(): FunctionMapper<JvmExpression> {
        return FunctionMapper.of {
            map(StandardLibrary.fileConstructor) { expression, functionMapper ->
                val value = toExpression(expression.args.first(), functionMapper)
                StaticMethodCall(path, "of", stringToPathMethod, listOf(value, EmptyStringArray))
            }
            map(StandardLibrary.listConstructor) { expression, target, functionMapper ->
                val args = expression.args.map { toExpression(it, functionMapper) }
                val elementType = target.argument
                NewArray(toTypeDescriptor(elementType), args)
            }
            mapOperator(StandardLibrary.stringConcatenation) { left, right ->
                concatString(left, right)
            }
            mapOperator(StandardLibrary.stringEquality) { left, right ->
                JvmStringEquality(left, right)
            }
            mapOperator(StandardLibrary.intAddition) { left, right ->
                JvmIntAddition(left, right)
            }
            mapOperator(StandardLibrary.intEquality) { left, right ->
                JvmIntEquality(left, right)
            }
            mapOperator(StandardLibrary.booleanEquality) { left, right ->
                JvmIntEquality(left, right)
            }
        }
    }

    private fun FunctionMapper.Builder<JvmExpression>.mapOperator(operator: OperatorDefinition, builder: (JvmExpression, JvmExpression) -> JvmExpression) {
        map(operator) { expression, functionMapper ->
            val left = toExpression(expression.args[0], functionMapper)
            val right = toExpression(expression.args[1], functionMapper)
            builder(left, right)
        }
    }

    private fun toTypeDescriptor(type: Type): TypeDescriptor {
        return when (type) {
            is Type.Boolean -> TypeDescriptor.Boolean
            is Type.Int -> TypeDescriptor.Int
            is Type.String -> TypeDescriptor.String
            is Type.File -> TypeDescriptor.String
            is Type.List -> objectArray
            is Type.Any -> TypeDescriptor.Object
        }
    }

    private fun fileToString(jvmExpression: JvmExpression): JvmExpression {
        val strValue = VirtualMethodCall(TypeDescriptor.Object, "toString", MethodDescriptor.of(TypeDescriptor.String), jvmExpression, emptyList())
        return concatString(JvmString("File("), concatString(strValue, JvmString(")")))
    }

    private fun intToString(expression: JvmExpression): JvmExpression {
        return StaticMethodCall(integer, "toString", intToStringMethod, listOf(expression))
    }

    private fun booleanToString(expression: JvmExpression): JvmExpression {
        return StaticMethodCall(boolean, "toString", boolToStringMethod, listOf(expression))
    }

    private fun listToString(elementType: Type, expression: JvmExpression): JvmExpression {
        return when (elementType) {
            is Type.String -> StaticMethodCall(arrays, "toString", objectArrayToStringMethod, listOf(expression))
            is Type.Int -> StaticMethodCall(arrays, "toString", intArrayToStringMethod, listOf(expression))
            is Type.Boolean -> StaticMethodCall(arrays, "toString", booleanArrayToStringMethod, listOf(expression))
            is Type.List -> StaticMethodCall(thisClass, listToString.name, listToString.descriptor, listOf(expression), listToString)
            else -> throw IllegalArgumentException()
        }
    }

    private fun toExpression(expression: Expression, functionMapper: FunctionMapper<JvmExpression>): JvmExpression {
        return when (expression) {
            is StringLiteral -> JvmString(expression.value)
            is IntLiteral -> JvmInt(expression.value)
            is BooleanLiteral -> if (expression.value) JvmTrue else JvmFalse
            is VariableReference -> JvmVariableReference(expression.target.name, toTypeDescriptor(expression.type))
            is FunctionCallExpression -> functionMapper.map(expression)
            is ReceiverFunctionCallExpression -> mapFunctionCall(expression, functionMapper)
        }
    }

    private fun mapFunctionCall(expression: ReceiverFunctionCallExpression, functionMapper: FunctionMapper<JvmExpression>): JvmExpression {
        return when (expression.target) {
            StringFunctions.toString -> toExpression(expression.receiver, functionMapper)

            IntFunctions.toString -> {
                val receiver = expression.receiver
                when (receiver) {
                    is IntLiteral -> JvmString(receiver.value.toString())
                    else -> intToString(toExpression(receiver, functionMapper))
                }
            }

            BooleanFunctions.toString -> {
                val receiver = expression.receiver
                when (receiver) {
                    is BooleanLiteral -> JvmString(receiver.value.toString())
                    else -> booleanToString(toExpression(receiver, functionMapper))
                }
            }

            ListFunctions.toString -> listToString((expression.receiver.type as Type.List).param, toExpression(expression.receiver, functionMapper))

            FileFunctions.toString -> fileToString(toExpression(expression.receiver, functionMapper))
            FileFunctions.readText -> {
                StaticMethodCall(files, "readString", pathToStringMethod, listOf(toExpression(expression.receiver, functionMapper)))
            }

            else -> throw IllegalArgumentException("Unknown ${expression.target.displayName} for ${expression.receiver.type}")
        }
    }

    private fun concatString(left: JvmExpression, right: JvmExpression): JvmExpression {
        return VirtualMethodCall(TypeDescriptor.String, "concat", stringToStringMethod, left, listOf(right))
    }

    private fun ClassBuilder.generate(method: JvmGeneratedMethod) {
        method(method.name, method.descriptor, method.flags) {
            val slots = mutableMapOf<String, LocalVariable>()
            for (operation in method.operations) {
                when (operation) {
                    is JvmVariableDeclaration -> {
                        val slot = allocateLocal(operation.type)
                        slots[operation.name] = slot
                        push(operation.value, slots)
                        storeLocal(slots.getValue(operation.name))
                    }

                    is InstanceMethodCall -> callMethod(operation, slots)
                    is ReturnVoid -> returnVoid()
                    is ReturnValue -> {
                        push(operation.value, slots)
                        returnValue(operation.type)
                    }
                }
            }
        }
    }

    private fun CodeBuilder.callMethod(methodCall: InstanceMethodCall, slots: Map<String, LocalVariable>) {
        push(methodCall.receiver, slots)
        for (expression in methodCall.arguments) {
            push(expression, slots)
        }
        invokeVirtual(methodCall.owner, methodCall.name, methodCall.descriptor)
    }

    private fun CodeBuilder.push(expression: JvmExpression, slots: Map<String, LocalVariable>) {
        when (expression) {
            is JvmString -> {
                loadConstant(expression.value)
            }

            is JvmInt -> {
                loadConstant(expression.value)
            }

            is JvmTrue -> {
                loadConstant(true)
            }

            is JvmFalse -> {
                loadConstant(false)
            }

            is JvmIntAddition -> {
                push(expression.left, slots)
                push(expression.right, slots)
                iadd()
            }

            is JvmIntEquality -> {
                push(expression.left, slots)
                push(expression.right, slots)
                intEquality()
            }

            is JvmStringEquality -> {
                push(expression.left, slots)
                push(expression.right, slots)
                invokeVirtual(TypeDescriptor.Object, "equals", objectToBoolMethod)
            }

            is JvmVariableReference -> {
                loadLocal(slots.getValue(expression.name))
            }

            is GetStaticField -> {
                getStaticField(expression.owner, expression.name, expression.type)
            }

            is StaticMethodCall -> {
                expression.args.forEach { push(it, slots) }
                invokeStatic(expression.owner, expression.name, expression.descriptor)
            }

            is VirtualMethodCall -> {
                push(expression.receiver, slots)
                expression.args.forEach { push(it, slots) }
                invokeVirtual(expression.owner, expression.name, expression.descriptor)
            }

            is ConstructorCall -> {
                new(expression.type)
                dup()
                expression.args.forEach { push(it, slots) }
                invokeSpecial(expression.type, "<init>", expression.descriptor)
            }

            is EmptyStringArray -> {
                loadConstant(0)
                newArray(TypeDescriptor.String)
            }

            is NewArray -> {
                loadConstant(expression.elements.size)
                newArray(expression.elementType)
                expression.elements.forEachIndexed { index, element ->
                    dup()
                    loadConstant(index)
                    push(element, slots)
                    arrayStore(expression.elementType)
                }
            }
        }
    }
}