package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.generator.Declaration
import net.rubygrapefruit.scripto.generator.FunctionMapper
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.ir.*

private val boolean = TypeDescriptor.ofClass("java.lang.Boolean")
private val integer = TypeDescriptor.ofClass("java.lang.Integer")
private val path = TypeDescriptor.ofInterface("java.nio.file.Path")
private val arrays = TypeDescriptor.ofClass("java.util.Arrays")
private val objectArray = TypeDescriptor.Object.arrayType()
private val stringArray = TypeDescriptor.String.arrayType()
private val intArray = TypeDescriptor.Int.arrayType()
private val booleanArray = TypeDescriptor.Boolean.arrayType()

private val stringBuilder = TypeDescriptor.ofClass("java.lang.StringBuilder")

private val voidMethod = MethodDescriptor.ofVoid()

private val toStringMethod = MethodDescriptor.of(TypeDescriptor.String)
private val intToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Int)
private val boolToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Boolean)
private val stringToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.String)
private val objectArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, objectArray)
private val intArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, intArray)
private val booleanArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Boolean.arrayType())

private val stringToStringBuilderMethod = MethodDescriptor.of(stringBuilder, TypeDescriptor.String)

private val stringCollector by lazy {
    JvmGeneratedInterface("StringCollector", TypeDescriptor.ofInterface("StringCollector"), ClassVisibility.PackageProtected)
}

private val collectValue by lazy {
    JvmGeneratedMethod(
        stringCollector,
        "collect",
        MethodDescriptor.ofVoid(TypeDescriptor.Object, stringBuilder),
        MethodVisibility.Public,
        false,
        emptyList(),
        emptyList()
    )
}

private val mainClass = JvmGeneratedClass("App", TypeDescriptor.ofClass("App"), ClassVisibility.Public, true)

private val listToString by lazy {
    val builder = JvmVariableReference("builder", TypeDescriptor.String)
    val length = JvmVariableReference("length", TypeDescriptor.Int)
    val index = JvmVariableReference("index", TypeDescriptor.Int)
    val listArg = JvmParameterReference(0, objectArray)
    val collectorArg = JvmParameterReference(1, stringCollector.descriptor)
    JvmGeneratedMethod(
        mainClass,
        "\$listToString",
        MethodDescriptor.of(TypeDescriptor.String, objectArray, stringCollector.descriptor),
        MethodVisibility.PackageProtected,
        true,
        emptyList(),
        listOf(
            JvmVariableDeclaration("length", TypeDescriptor.Int, JvmArrayLength(listArg)),
            IfIntsEqual(length, JvmInt(0), listOf(ReturnValue(TypeDescriptor.String, JvmString("[]")))),
            JvmVariableDeclaration("builder", stringBuilder, ConstructorCall(stringBuilder, voidMethod, emptyList())),
            appendToStringBuilder(builder, JvmString("[")),
            JvmVariableDeclaration("index", TypeDescriptor.Int, JvmInt(0)),
            WhileIntLessThan(
                index, length, listOf(
                    IfIntGreaterThan(
                        index, JvmInt(0), listOf(
                            appendToStringBuilder(builder, JvmString(", "))
                        )
                    ),
                    VirtualMethodCall(
                        collectValue, collectorArg, listOf(
                            JvmGetArrayElement(TypeDescriptor.Object, listArg, index),
                            builder
                        )
                    ),
                    JvmIncrementVariable("index")
                )
            ),
            appendToStringBuilder(builder, JvmString("]")),
            ReturnValue(
                TypeDescriptor.String,
                VirtualMethodCall(stringBuilder, "toString", toStringMethod, builder, emptyList())
            )
        )
    )
}

@OptIn(ExperimentalStdlibApi::class)
class JvmGenerator {
    private val files = TypeDescriptor.ofClass("java.nio.file.Files")
    private val printStream = TypeDescriptor.ofClass("java.io.PrintStream")
    private val system = TypeDescriptor.ofClass("java.lang.System")

    private val stringToVoidMethod = MethodDescriptor.ofVoid(TypeDescriptor.String)

    private val stringToPathMethod = MethodDescriptor.of(path, TypeDescriptor.String, stringArray)
    private val objectToBoolMethod = MethodDescriptor.of(TypeDescriptor.Boolean, TypeDescriptor.Object)
    private val pathToStringMethod = MethodDescriptor.of(TypeDescriptor.String, path)

    private val systemOut = GetStaticField(system, "out", printStream)

    private val encodedTypes = mutableMapOf<Type, EncodedType>()

    fun generate(program: Program, output: Directory): GeneratedClass {
        val functionMapper = createFunctionMapper()
        val operationMapper = createOperationMapper(functionMapper)

        val operations = mutableListOf<JvmOperation>()
        operationMapper.mapInto(program.operations, operations)
        operations.add(ReturnVoid)

        val mainMethod = JvmGeneratedMethod(
            mainClass,
            "main",
            MethodDescriptor.ofVoid(stringArray),
            MethodVisibility.Public,
            true,
            listOf("args"),
            operations
        )

        val classes = mutableMapOf<JvmGeneratedType, MutableList<JvmGeneratedMethod>>()
        val queue = Declaration.Queue<JvmDeclaration>()
        queue.add(mainMethod)
        queue.visitAll { declaration ->
            if (declaration is JvmGeneratedMethod) {
                classes.getOrPutIfNull(declaration.owner) { mutableListOf() }.add(declaration)
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
                val type = toEncodedType(declaration.type).jvmType
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
                val elementType = toEncodedType(target.argument).jvmType
                NewArray(elementType, args)
            }
            mapOperator(StandardLibrary.stringConcatenation) { left, right ->
                concatString(left, right)
            }
            mapOperator(StandardLibrary.stringEquality) { left, right ->
                VirtualMethodCall(TypeDescriptor.Object, "equals", objectToBoolMethod, left, listOf(right))
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

    private fun toEncodedType(type: Type): EncodedType {
        return encodedTypes.getOrPutIfNull(type) {
            when (type) {
                is Type.Boolean -> EncodedType.Boolean
                is Type.Int -> EncodedType.Int
                is Type.String -> EncodedType.String
                is Type.File -> EncodedType.File
                is Type.List -> when (type.param) {
                    is Type.String -> EncodedType.StringList
                    is Type.Int -> EncodedType.IntList
                    is Type.Boolean -> EncodedType.BooleanList
                    else -> EncodedType.ObjectList(toEncodedType(type.param) as EncodedType.ReferenceType)
                }

                is Type.Any -> EncodedType.Any
            }
        }
    }

    private fun toExpression(expression: Expression, functionMapper: FunctionMapper<JvmExpression>): JvmExpression {
        return when (expression) {
            is StringLiteral -> JvmString(expression.value)
            is IntLiteral -> JvmInt(expression.value)
            is BooleanLiteral -> if (expression.value) JvmTrue else JvmFalse
            is VariableReference -> JvmVariableReference(expression.name, toEncodedType(expression.type).jvmType)
            is FunctionCallExpression -> functionMapper.map(expression)
            is ReceiverFunctionCallExpression -> mapFunctionCall(expression, functionMapper)
        }
    }

    private fun mapFunctionCall(expression: ReceiverFunctionCallExpression, functionMapper: FunctionMapper<JvmExpression>): JvmExpression {
        return when (expression.target) {
            StringFunctions.toString -> EncodedType.String.toString(toExpression(expression.receiver, functionMapper))

            IntFunctions.toString -> {
                val receiver = expression.receiver
                when (receiver) {
                    is IntLiteral -> JvmString(receiver.value.toString())
                    else -> EncodedType.Int.toString(toExpression(receiver, functionMapper))
                }
            }

            BooleanFunctions.toString -> {
                val receiver = expression.receiver
                when (receiver) {
                    is BooleanLiteral -> JvmString(receiver.value.toString())
                    else -> EncodedType.Boolean.toString(toExpression(receiver, functionMapper))
                }
            }

            ListFunctions.toString -> toEncodedType(expression.receiver.type).toString(toExpression(expression.receiver, functionMapper))

            FileFunctions.toString -> EncodedType.File.toString(toExpression(expression.receiver, functionMapper))

            FileFunctions.readText -> {
                StaticMethodCall(files, "readString", pathToStringMethod, listOf(toExpression(expression.receiver, functionMapper)))
            }

            else -> throw IllegalArgumentException("Unknown ${expression.target.displayName} for ${expression.receiver.type}")
        }
    }
}

private sealed interface EncodedType {
    /**
     * Prefix for types generated from this type.
     */
    val typeNamePrefix: kotlin.String

    val jvmType: TypeDescriptor

    sealed interface ReferenceType : EncodedType {
        override val jvmType: TypeDescriptor.ReferenceType
    }

    fun toString(receiver: JvmExpression): JvmExpression

    object String : ReferenceType {
        override val typeNamePrefix: kotlin.String
            get() = "String"

        override val jvmType: TypeDescriptor.ClassType
            get() = TypeDescriptor.String

        override fun toString(receiver: JvmExpression): JvmExpression {
            return receiver
        }
    }

    object StringList : ReferenceType {
        override val typeNamePrefix: kotlin.String
            get() = "StringList"

        override val jvmType: TypeDescriptor.ReferenceType
            get() = stringArray

        override fun toString(receiver: JvmExpression): JvmExpression {
            return StaticMethodCall(arrays, "toString", objectArrayToStringMethod, listOf(receiver))
        }
    }

    object Int : EncodedType {
        override val typeNamePrefix: kotlin.String
            get() = "Int"

        override val jvmType: TypeDescriptor
            get() = TypeDescriptor.Int

        override fun toString(receiver: JvmExpression): JvmExpression {
            return StaticMethodCall(integer, "toString", intToStringMethod, listOf(receiver))
        }
    }

    object IntList : ReferenceType {
        override val typeNamePrefix: kotlin.String
            get() = "IntList"

        override val jvmType: TypeDescriptor.ReferenceType
            get() = intArray

        override fun toString(receiver: JvmExpression): JvmExpression {
            return StaticMethodCall(arrays, "toString", intArrayToStringMethod, listOf(receiver))
        }
    }

    object Boolean : EncodedType {
        override val typeNamePrefix: kotlin.String
            get() = "Boolean"

        override val jvmType: TypeDescriptor
            get() = TypeDescriptor.Boolean

        override fun toString(receiver: JvmExpression): JvmExpression {
            return StaticMethodCall(boolean, "toString", boolToStringMethod, listOf(receiver))
        }
    }

    object BooleanList : ReferenceType {
        override val typeNamePrefix: kotlin.String
            get() = "BooleanList"

        override val jvmType: TypeDescriptor.ReferenceType
            get() = booleanArray

        override fun toString(receiver: JvmExpression): JvmExpression {
            return StaticMethodCall(arrays, "toString", booleanArrayToStringMethod, listOf(receiver))
        }
    }

    object File : ReferenceType {
        override val typeNamePrefix: kotlin.String
            get() = "File"

        override val jvmType: TypeDescriptor.ObjectType
            get() = path

        override fun toString(receiver: JvmExpression): JvmExpression {
            val strValue = VirtualMethodCall(TypeDescriptor.Object, "toString", MethodDescriptor.of(TypeDescriptor.String), receiver, emptyList())
            return concatString(JvmString("File("), concatString(strValue, JvmString(")")))
        }
    }

    class ObjectList(val elementType: ReferenceType) : ReferenceType {
        val collector by lazy { collector(elementType) }

        override val typeNamePrefix: kotlin.String = "List${elementType.typeNamePrefix}"

        override val jvmType: TypeDescriptor.ReferenceType
            get() = elementType.jvmType.arrayType()

        override fun toString(receiver: JvmExpression): JvmExpression {
            return StaticMethodCall(listToString, listOf(receiver, collector))
        }
    }

    object Any : ReferenceType {
        override val typeNamePrefix: kotlin.String
            get() = "Any"

        override val jvmType: TypeDescriptor.ReferenceType
            get() = TypeDescriptor.Object

        override fun toString(receiver: JvmExpression): JvmExpression {
            return JvmString("<any>")
        }
    }
}

private fun concatString(left: JvmExpression, right: JvmExpression): JvmExpression {
    return VirtualMethodCall(TypeDescriptor.String, "concat", stringToStringMethod, left, listOf(right))
}

private fun appendToStringBuilder(receiver: JvmExpression, value: JvmExpression): JvmOperation {
    // pop the return value of append() from the stack
    return JvmDiscard(VirtualMethodCall(stringBuilder, "append", stringToStringBuilderMethod, receiver, listOf(value)))
}

private fun collector(type: EncodedType.ReferenceType): JvmExpression {
    val className = "${type.typeNamePrefix}StringCollector"
    val collector = JvmGeneratedClass(
        className,
        TypeDescriptor.ofClass(className),
        ClassVisibility.PackageProtected,
        true,
        listOf(stringCollector)
    )
    val collectMethod = JvmGeneratedMethod(
        collector,
        collectValue.name,
        collectValue.descriptor,
        MethodVisibility.Public,
        false,
        emptyList(),
        listOf(
            appendToStringBuilder(
                JvmParameterReference(2, stringBuilder),
                type.toString(JvmCast(type.jvmType, JvmParameterReference(1, TypeDescriptor.Object)))
            ),
            ReturnVoid
        )
    )
    return ConstructorCall(collector.descriptor, voidMethod, emptyList(), collectMethod)
}
