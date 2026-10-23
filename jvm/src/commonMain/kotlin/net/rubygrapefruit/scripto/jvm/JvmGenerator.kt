package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.FunctionMapper
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.ir.*

@OptIn(ExperimentalStdlibApi::class)
class JvmGenerator {
    private val integer = TypeDescriptor.ofClass("java.lang.Integer")
    private val boolean = TypeDescriptor.ofClass("java.lang.Boolean")
    private val files = TypeDescriptor.ofClass("java.nio.file.Files")
    private val path = TypeDescriptor.ofInterface("java.nio.file.Path")
    private val arrays = TypeDescriptor.ofClass("java.util.Arrays")
    private val objectArray = TypeDescriptor.Object.arrayType()
    private val stringArray = TypeDescriptor.String.arrayType()
    private val printStream = TypeDescriptor.ofClass("java.io.PrintStream")
    private val system = TypeDescriptor.ofClass("java.lang.System")
    private val stringBuilder = TypeDescriptor.ofClass("java.lang.StringBuilder")

    private val voidMethod = MethodDescriptor.ofVoid()
    private val toStringMethod = MethodDescriptor.of(TypeDescriptor.String)
    private val stringToVoidMethod = MethodDescriptor.ofVoid(TypeDescriptor.String)
    private val stringToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.String)
    private val stringToStringBuilder = MethodDescriptor.of(stringBuilder, TypeDescriptor.String)

    private val stringToPathMethod = MethodDescriptor.of(path, TypeDescriptor.String, stringArray)
    private val intToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Int)
    private val boolToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Boolean)
    private val objectToBoolMethod = MethodDescriptor.of(TypeDescriptor.Boolean, TypeDescriptor.Object)
    private val pathToStringMethod = MethodDescriptor.of(TypeDescriptor.String, path)
    private val objectArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, objectArray)
    private val intArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Int.arrayType())
    private val booleanArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Boolean.arrayType())

    private val systemOut = GetStaticField(system, "out", printStream)

    private val stringCollector by lazy {
        JvmGeneratedClass("StringCollector", TypeDescriptor.ofInterface("StringCollector"), ClassFlags.PackageProtected)
    }

    private val collectValue by lazy {
        JvmGeneratedMethod(stringCollector, "collect", MethodDescriptor.ofVoid(TypeDescriptor.Object, stringBuilder), MethodFlags.Public, emptyList())
    }

    private val mainClass = JvmGeneratedClass("App", TypeDescriptor.ofClass("App"), ClassFlags.PublicFinal)

    private val listToString by lazy {
        val builder = JvmVariableReference("builder", TypeDescriptor.String)
        val length = JvmVariableReference("length", TypeDescriptor.Int)
        val index = JvmVariableReference("index", TypeDescriptor.Int)
        val listArg = JvmParameterReference(0, objectArray)
        val collectorArg = JvmParameterReference(1, stringCollector.descriptor)
        JvmGeneratedMethod(
            mainClass, "\$listToString", MethodDescriptor.of(TypeDescriptor.String, objectArray, stringCollector.descriptor), MethodFlags.PrivateStatic, listOf(
                JvmVariableDeclaration("length", TypeDescriptor.Int, JvmArrayLength(listArg)),
                IfIntsEqual(length, JvmInt(0), listOf(ReturnValue(TypeDescriptor.String, JvmString("[]")))),
                JvmVariableDeclaration("builder", stringBuilder, ConstructorCall(stringBuilder, voidMethod, emptyList())),
                VirtualMethodCall(stringBuilder, "append", stringToStringBuilder, builder, listOf(JvmString("["))),
                JvmVariableDeclaration("index", TypeDescriptor.Int, JvmInt(0)),
                VirtualMethodCall(
                    collectValue, collectorArg, listOf(
                        JvmGetArrayElement(TypeDescriptor.Object, listArg, index),
                        builder
                    )
                ),
                VirtualMethodCall(stringBuilder, "append", stringToStringBuilder, builder, listOf(JvmString("]"))),
                ReturnValue(
                    TypeDescriptor.String,
                    VirtualMethodCall(stringBuilder, "toString", toStringMethod, builder, emptyList())
                )
            )
        )
    }

    fun generate(program: Program, output: Directory): GeneratedClass {
        val functionMapper = createFunctionMapper()
        val operationMapper = createOperationMapper(functionMapper)

        val operations = mutableListOf<JvmOperation>()
        operationMapper.mapInto(program.operations, operations)
        operations.add(ReturnVoid)

        val stringArray = stringArray

        val mainMethod = JvmGeneratedMethod(mainClass, "main", MethodDescriptor.ofVoid(stringArray), MethodFlags.PublicStatic, operations)

        val classes = mutableMapOf<JvmGeneratedClass, MutableList<JvmGeneratedMethod>>()
        val seen = mutableSetOf<JvmDeclaration>()
        val queue = mutableListOf<JvmDeclaration>()
        queue.add(mainMethod)
        while (queue.isNotEmpty()) {
            val declaration = queue.removeFirst()
            if (seen.add(declaration)) {
                declaration.visitDeclarations {
                    if (!seen.contains(it)) {
                        queue.add(it)
                    }
                }
                if (declaration is JvmGeneratedMethod) {
                    classes.getOrPutIfNull(declaration.owner) { mutableListOf() }.add(declaration)
                }
            }
        }

        val classFiles = classes.map { (generatedClass, methods) ->
            generate(generatedClass, methods, output)
        }

        return GeneratedClass(mainClass.name, classFiles)
    }

    private fun createOperationMapper(functionMapper: FunctionMapper<JvmExpression>): OperationMapper<JvmOperation> {
        return OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                VirtualMethodCall(printStream, "println", voidMethod, systemOut, emptyList())
            }
            map(StandardLibrary.printlnOneArg) { call ->
                val call = StandardLibrary.convertPrintln(call)
                val args = call.arguments.map { toExpression(it, functionMapper) }
                VirtualMethodCall(printStream, "println", stringToVoidMethod, systemOut, args)
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
            is Type.String -> stringListToString(expression)
            is Type.Int -> StaticMethodCall(arrays, "toString", intArrayToStringMethod, listOf(expression))
            is Type.Boolean -> StaticMethodCall(arrays, "toString", booleanArrayToStringMethod, listOf(expression))
            is Type.List -> StaticMethodCall(listToString, listOf(expression, stringListCollector()))
            is Type.Any -> StaticMethodCall(listToString, listOf(expression, JvmNull))
            is Type.File -> StaticMethodCall(listToString, listOf(expression, JvmNull))
        }
    }

    private fun stringListToString(expression: JvmExpression): JvmExpression {
        return StaticMethodCall(arrays, "toString", objectArrayToStringMethod, listOf(expression))
    }

    private fun stringListCollector(): JvmExpression {
        val stringListCollector = JvmGeneratedClass(
            "StringStringCollector",
            TypeDescriptor.ofClass("StringStringCollector"),
            ClassFlags.PackageProtected,
            listOf(stringCollector)
        )
        val collectMethod = JvmGeneratedMethod(
            stringListCollector, collectValue.name, collectValue.descriptor, collectValue.flags, listOf(
                VirtualMethodCall(
                    stringBuilder, "append", stringToStringBuilder, JvmParameterReference(2, stringBuilder), listOf(
                        stringListToString(JvmCast(stringArray, JvmParameterReference(1, TypeDescriptor.Object)))
                    )
                ),
                ReturnVoid
            )
        )
        return ConstructorCall(stringListCollector.descriptor, voidMethod, emptyList(), collectMethod)
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

    private fun generate(generatedClass: JvmGeneratedClass, methods: List<JvmGeneratedMethod>, output: Directory): RegularFile {
        val classFile = output.file("${generatedClass.name}.class")
        val needsConstructor = !generatedClass.descriptor.isInterface && methods.any { !it.flags.isStatic }
        val interfaces = generatedClass.interfaces.map { it.descriptor }

        ClassFileWriter().writeTo(generatedClass.descriptor, generatedClass.flags, interfaces, classFile) {
            if (needsConstructor) {
                constructor(MethodFlags.Public)
            }
            for (method in methods) {
                generate(method)
            }
        }
        return classFile
    }

    private fun ClassBuilder.generate(method: JvmGeneratedMethod) {
        if (method.operations.isEmpty()) {
            method(method.name, method.descriptor, method.flags)
        } else {
            method(method.name, method.descriptor, method.flags) {
                val slots = mutableMapOf<String, LocalVariable>()
                for (operation in method.operations) {
                    generate(operation, slots)
                }
            }
        }
    }

    private fun CodeBuilder.generate(operation: JvmOperation, slots: MutableMap<String, LocalVariable>) {
        when (operation) {
            is JvmVariableDeclaration -> {
                val slot = allocateLocal(operation.type)
                slots[operation.name] = slot
                push(operation.value, slots)
                storeLocal(slots.getValue(operation.name))
            }

            is VirtualMethodCall -> callMethod(operation, slots)
            is IfIntsEqual -> {
                push(operation.left, slots)
                push(operation.right, slots)
                intEquality {
                    for (nested in operation.whenTrue) {
                        generate(nested, slots)
                    }
                }
            }

            is ReturnVoid -> returnVoid()
            is ReturnValue -> {
                push(operation.value, slots)
                returnValue(operation.type)
            }
        }
    }

    private fun CodeBuilder.callMethod(methodCall: VirtualMethodCall, slots: Map<String, LocalVariable>) {
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

            is JvmNull -> {
                loadNull()
            }

            is JvmIntAddition -> {
                push(expression.left, slots)
                push(expression.right, slots)
                iadd()
            }

            is JvmIntEquality -> {
                push(expression.left, slots)
                push(expression.right, slots)
                intEquality({
                    loadConstant(true)
                }, {
                    loadConstant(false)
                })
            }

            is JvmStringEquality -> {
                push(expression.left, slots)
                push(expression.right, slots)
                invokeVirtual(TypeDescriptor.Object, "equals", objectToBoolMethod)
            }

            is JvmCast -> {
                push(expression.value, slots)
                checkCast(expression.type)
            }

            is JvmVariableReference -> {
                loadLocal(slots.getValue(expression.name))
            }

            is JvmParameterReference -> {
                loadParameter(expression.index, expression.type)
            }

            is GetStaticField -> {
                getStaticField(expression.owner, expression.name, expression.type)
            }

            is StaticMethodCall -> {
                expression.arguments.forEach { push(it, slots) }
                invokeStatic(expression.owner, expression.name, expression.descriptor)
            }

            is VirtualMethodCall -> {
                push(expression.receiver, slots)
                expression.arguments.forEach { push(it, slots) }
                invokeVirtual(expression.owner, expression.name, expression.descriptor)
            }

            is ConstructorCall -> {
                new(expression.type)
                dup()
                expression.arguments.forEach { push(it, slots) }
                invokeSpecial(expression.type, "<init>", expression.descriptor)
            }

            is JvmArrayLength -> {
                push(expression.value, slots)
                arrayLength()
            }

            is JvmGetArrayElement -> {
                push(expression.value, slots)
                push(expression.index, slots)
                arrayLoad(expression.elementType)
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