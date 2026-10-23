package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.scripto.generator.Declaration
import net.rubygrapefruit.scripto.generator.FunctionMapper
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.ir.*

private val boolean = TypeDescriptor.ofClass("java.lang.Boolean")
private val integer = TypeDescriptor.ofClass("java.lang.Integer")
private val arrays = TypeDescriptor.ofClass("java.util.Arrays")
private val objectArray = TypeDescriptor.Object.arrayType()
private val stringArray = TypeDescriptor.String.arrayType()
private val intArray = TypeDescriptor.Int.arrayType()
private val booleanArray = TypeDescriptor.Boolean.arrayType()

private val intToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Int)
private val boolToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Boolean)
private val stringToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.String)
private val objectArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, objectArray)
private val intArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, intArray)
private val booleanArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Boolean.arrayType())

@OptIn(ExperimentalStdlibApi::class)
class JvmGenerator {
    private val files = TypeDescriptor.ofClass("java.nio.file.Files")
    private val path = TypeDescriptor.ofInterface("java.nio.file.Path")
    private val printStream = TypeDescriptor.ofClass("java.io.PrintStream")
    private val system = TypeDescriptor.ofClass("java.lang.System")
    private val stringBuilder = TypeDescriptor.ofClass("java.lang.StringBuilder")

    private val voidMethod = MethodDescriptor.ofVoid()
    private val toStringMethod = MethodDescriptor.of(TypeDescriptor.String)
    private val stringToVoidMethod = MethodDescriptor.ofVoid(TypeDescriptor.String)
    private val stringToStringBuilder = MethodDescriptor.of(stringBuilder, TypeDescriptor.String)

    private val stringToPathMethod = MethodDescriptor.of(path, TypeDescriptor.String, stringArray)
    private val objectToBoolMethod = MethodDescriptor.of(TypeDescriptor.Boolean, TypeDescriptor.Object)
    private val pathToStringMethod = MethodDescriptor.of(TypeDescriptor.String, path)

    private val systemOut = GetStaticField(system, "out", printStream)

    private val stringCollector by lazy {
        JvmGeneratedInterface("StringCollector", TypeDescriptor.ofInterface("StringCollector"), ClassVisibility.PackageProtected)
    }

    private val collectValue by lazy {
        JvmGeneratedMethod(stringCollector, "collect", MethodDescriptor.ofVoid(TypeDescriptor.Object, stringBuilder), MethodVisibility.Public, false, emptyList())
    }

    private val mainClass = JvmGeneratedClass("App", TypeDescriptor.ofClass("App"), ClassVisibility.Public, true)

    private val listToString by lazy {
        val builder = JvmVariableReference("builder", TypeDescriptor.String)
        val length = JvmVariableReference("length", TypeDescriptor.Int)
        val index = JvmVariableReference("index", TypeDescriptor.Int)
        val listArg = JvmParameterReference(0, objectArray)
        val collectorArg = JvmParameterReference(1, stringCollector.descriptor)
        JvmGeneratedMethod(
            mainClass, "\$listToString", MethodDescriptor.of(TypeDescriptor.String, objectArray, stringCollector.descriptor), MethodVisibility.Private, true, listOf(
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

    private fun appendToStringBuilder(receiver: JvmExpression, value: JvmExpression): JvmOperation {
        // pop the return value of append() from the stack
        return JvmDiscard(VirtualMethodCall(stringBuilder, "append", stringToStringBuilder, receiver, listOf(value)))
    }

    fun generate(program: Program, output: Directory): GeneratedClass {
        val functionMapper = createFunctionMapper()
        val operationMapper = createOperationMapper(functionMapper)

        val operations = mutableListOf<JvmOperation>()
        operationMapper.mapInto(program.operations, operations)
        operations.add(ReturnVoid)

        val mainMethod = JvmGeneratedMethod(mainClass, "main", MethodDescriptor.ofVoid(stringArray), MethodVisibility.Public, true, operations)

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
                val elementType = toTypeDescriptor(target.argument)
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

    private fun toTypeDescriptor(type: Type): TypeDescriptor {
        return when (type) {
            is Type.Boolean -> TypeDescriptor.Boolean
            is Type.Int -> TypeDescriptor.Int
            is Type.String -> TypeDescriptor.String
            is Type.File -> TypeDescriptor.String
            is Type.List -> TypeDescriptor.Object
            is Type.Any -> TypeDescriptor.Object
        }
    }

    private fun listToString(elementType: Type, expression: JvmExpression): JvmExpression {
        return when (elementType) {
            is Type.String -> EncodedType.StringList.toString(expression)
            is Type.Int -> EncodedType.IntList.toString(expression)
            is Type.Boolean -> EncodedType.BooleanList.toString(expression)
            is Type.List -> {
                val collector = when (elementType.param) {
                    is Type.Int -> intListStringCollector
                    else -> stringListStringCollector
                }
                StaticMethodCall(listToString, listOf(expression, collector))
            }

            is Type.Any -> StaticMethodCall(listToString, listOf(expression, JvmNull))
            is Type.File -> StaticMethodCall(listToString, listOf(expression, JvmNull))
        }
    }

    /**
     * int[] -> String
     */
    private val intListStringCollector by lazy { collector(EncodedType.IntList) }

    /**
     * String[] -> String
     */
    private val stringListStringCollector by lazy { collector(EncodedType.StringList) }

    private fun collector(type: EncodedType.ReferenceType): JvmExpression {
        val className = "${type.namePrefix}StringCollector"
        val collector = JvmGeneratedClass(
            className,
            TypeDescriptor.ofClass(className),
            ClassVisibility.PackageProtected,
            true,
            listOf(stringCollector)
        )
        val collectMethod = JvmGeneratedMethod(
            collector, collectValue.name, collectValue.descriptor, MethodVisibility.Public, false, listOf(
                appendToStringBuilder(
                    JvmParameterReference(2, stringBuilder),
                    type.toString(JvmCast(type.jvmType, JvmParameterReference(1, TypeDescriptor.Object)))
                ),
                ReturnVoid
            )
        )
        return ConstructorCall(collector.descriptor, voidMethod, emptyList(), collectMethod)
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

            ListFunctions.toString -> listToString((expression.receiver.type as Type.List).param, toExpression(expression.receiver, functionMapper))

            FileFunctions.toString -> EncodedType.File.toString(toExpression(expression.receiver, functionMapper))

            FileFunctions.readText -> {
                StaticMethodCall(files, "readString", pathToStringMethod, listOf(toExpression(expression.receiver, functionMapper)))
            }

            else -> throw IllegalArgumentException("Unknown ${expression.target.displayName} for ${expression.receiver.type}")
        }
    }

    private sealed interface EncodedType {
        val namePrefix: kotlin.String

        sealed interface ReferenceType : EncodedType {
            val jvmType: TypeDescriptor.ReferenceType
        }

        fun toString(receiver: JvmExpression): JvmExpression

        object String : ReferenceType {
            override val namePrefix: kotlin.String
                get() = "String"

            override val jvmType: TypeDescriptor.ClassType
                get() = TypeDescriptor.String

            override fun toString(receiver: JvmExpression): JvmExpression {
                return receiver
            }
        }

        object StringList : ReferenceType {
            override val namePrefix: kotlin.String
                get() = "StringList"

            override val jvmType: TypeDescriptor.ReferenceType
                get() = stringArray

            override fun toString(receiver: JvmExpression): JvmExpression {
                return StaticMethodCall(arrays, "toString", objectArrayToStringMethod, listOf(receiver))
            }
        }

        object Int : EncodedType {
            override val namePrefix: kotlin.String
                get() = "Int"

            override fun toString(receiver: JvmExpression): JvmExpression {
                return StaticMethodCall(integer, "toString", intToStringMethod, listOf(receiver))
            }
        }

        object IntList : ReferenceType {
            override val namePrefix: kotlin.String
                get() = "IntList"

            override val jvmType: TypeDescriptor.ReferenceType
                get() = intArray

            override fun toString(receiver: JvmExpression): JvmExpression {
                return StaticMethodCall(arrays, "toString", intArrayToStringMethod, listOf(receiver))
            }
        }

        object Boolean : EncodedType {
            override val namePrefix: kotlin.String
                get() = "Boolean"

            override fun toString(receiver: JvmExpression): JvmExpression {
                return StaticMethodCall(boolean, "toString", boolToStringMethod, listOf(receiver))
            }
        }

        object BooleanList : ReferenceType {
            override val namePrefix: kotlin.String
                get() = "BooleanList"

            override val jvmType: TypeDescriptor.ReferenceType
                get() = booleanArray

            override fun toString(receiver: JvmExpression): JvmExpression {
                return StaticMethodCall(arrays, "toString", booleanArrayToStringMethod, listOf(receiver))
            }
        }

        object File : ReferenceType {
            override val namePrefix: kotlin.String
                get() = "File"

            override val jvmType: TypeDescriptor.ClassType
                get() = TypeDescriptor.String

            override fun toString(receiver: JvmExpression): JvmExpression {
                val strValue = VirtualMethodCall(TypeDescriptor.Object, "toString", MethodDescriptor.of(TypeDescriptor.String), receiver, emptyList())
                return concatString(JvmString("File("), concatString(strValue, JvmString(")")))
            }
        }
    }
}

private fun concatString(left: JvmExpression, right: JvmExpression): JvmExpression {
    return VirtualMethodCall(TypeDescriptor.String, "concat", stringToStringMethod, left, listOf(right))
}
