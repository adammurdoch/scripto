package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.ir.*

class JvmGenerator {
    private val integer = TypeDescriptor.ofClass("java.lang.Integer")
    private val boolean = TypeDescriptor.ofClass("java.lang.Boolean")
    private val files = TypeDescriptor.ofClass("java.nio.file.Files")
    private val path = TypeDescriptor.ofInterface("java.nio.file.Path")
    private val arrays = TypeDescriptor.ofClass("java.util.Arrays")
    private val objectArray = TypeDescriptor.Object.arrayType()
    private val intToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Int)
    private val boolToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Boolean)
    private val objectToBoolMethod = MethodDescriptor.of(TypeDescriptor.Boolean, TypeDescriptor.Object)
    private val pathToStringMethod = MethodDescriptor.of(TypeDescriptor.String, path)
    private val stringToPathMethod = MethodDescriptor.of(path, TypeDescriptor.String, TypeDescriptor.String.arrayType())
    private val stringToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.String)
    private val objectArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, objectArray)
    private val intArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Int.arrayType())
    private val booleanArrayToStringMethod = MethodDescriptor.of(TypeDescriptor.String, TypeDescriptor.Boolean.arrayType())

    fun generate(program: Program, output: RegularFile): GeneratedClass {
        ClassFileWriter().writeTo("App", output) { generate(program) }
        return GeneratedClass("App")
    }

    private fun ClassBuilder.generate(program: Program) {
        val operationMapper = createOperationMapper()

        val statements = operationMapper.map(program.operations)
        val jvmProgram = JvmProgram(statements)

        val stringArray = TypeDescriptor.String.arrayType()
        method("main", MethodDescriptor.ofVoid(stringArray), MethodFlags.PublicStatic) {
            val slots = mutableMapOf<String, LocalVariable>()
            for (declaration in jvmProgram.variables) {
                val slot = allocateLocal(declaration.type)
                slots[declaration.name] = slot
            }
            for (operation in jvmProgram.operations) {
                when (operation) {
                    is JvmVariableDeclaration -> {
                        push(operation.value, slots)
                        storeLocal(slots.getValue(operation.name))
                    }

                    is InstanceMethodCall -> {
                        callMethod(operation, slots)
                    }
                }
            }
            return_()
        }
    }

    private fun createOperationMapper(): OperationMapper<JvmOperation> {
        val printStream = TypeDescriptor.ofClass("java.io.PrintStream")
        val system = TypeDescriptor.ofClass("java.lang.System")

        val voidMethod = MethodDescriptor.ofVoid()
        val stringToVoidMethod = MethodDescriptor.ofVoid(TypeDescriptor.String)

        val systemOut = StaticField(system, "out", printStream)

        return OperationMapper.of {
            map(StandardLibrary.printlnNoArgs) {
                InstanceMethodCall(printStream, "println", voidMethod, systemOut, emptyList())
            }
            map(StandardLibrary.printlnOneArg) { call ->
                val call = StandardLibrary.convertPrintln(call)
                val args = call.arguments.map { toExpression(it) }
                InstanceMethodCall(printStream, "println", stringToVoidMethod, systemOut, args)
            }
            map { declaration ->
                val type = toTypeDescriptor(declaration.type)
                JvmVariableDeclaration(declaration, type, toExpression(declaration.value))
            }
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
            is Type.String, is Type.Any, is Type.List -> StaticMethodCall(arrays, "toString", objectArrayToStringMethod, listOf(expression))
            is Type.Int -> StaticMethodCall(arrays, "toString", intArrayToStringMethod, listOf(expression))
            is Type.Boolean -> StaticMethodCall(arrays, "toString", booleanArrayToStringMethod, listOf(expression))
            else -> throw IllegalArgumentException()
        }
    }

    private fun toExpression(expression: Expression): JvmExpression {
        return when (expression) {
            is StringLiteral -> JvmString(expression.value)
            is IntLiteral -> JvmInt(expression.value)
            is BooleanLiteral -> if (expression.value) JvmTrue else JvmFalse
            is VariableReference -> JvmVariableReference(expression)
            is FunctionCallExpression -> mapFunctionCall(expression)
            is ReceiverFunctionCallExpression -> mapFunctionCall(expression)
        }
    }

    private fun mapFunctionCall(expression: FunctionCallExpression): JvmExpression {
        val target = expression.target
        return when (target) {
            StandardLibrary.fileConstructor -> {
                val value = toExpression(expression.args.first())
                StaticMethodCall(path, "of", stringToPathMethod, listOf(value, EmptyStringArray))
            }

            StandardLibrary.intAddition -> JvmIntAddition(toExpression(expression.args[0]), toExpression(expression.args[1]))
            StandardLibrary.stringConcatenation -> {
                val left = toExpression(expression.args[0])
                val right = toExpression(expression.args[1])
                concatString(left, right)
            }

            StandardLibrary.stringEquality -> JvmStringEquality(toExpression(expression.args[0]), toExpression(expression.args[1]))
            StandardLibrary.intEquality -> JvmIntEquality(toExpression(expression.args[0]), toExpression(expression.args[1]))
            StandardLibrary.booleanEquality -> JvmIntEquality(toExpression(expression.args[0]), toExpression(expression.args[1]))

            is RealizedFunctionDefinition -> {
                when (target.definition) {
                    StandardLibrary.listConstructor -> {
                        val args = expression.args.map { toExpression(it) }
                        val elementType = target.argument
                        NewArray(toTypeDescriptor(elementType), args)
                    }

                    else -> throw IllegalArgumentException("Unknown ${target.displayName}")
                }
            }

            else -> throw IllegalArgumentException("Unknown ${target.displayName}")
        }
    }

    private fun mapFunctionCall(expression: ReceiverFunctionCallExpression): JvmExpression {
        return when (expression.target) {
            StringFunctions.toString -> toExpression(expression.receiver)

            IntFunctions.toString -> {
                val receiver = expression.receiver
                when (receiver) {
                    is IntLiteral -> JvmString(receiver.value.toString())
                    else -> intToString(toExpression(receiver))
                }
            }

            BooleanFunctions.toString -> {
                val receiver = expression.receiver
                when (receiver) {
                    is BooleanLiteral -> JvmString(receiver.value.toString())
                    else -> booleanToString(toExpression(receiver))
                }
            }

            ListFunctions.toString -> listToString((expression.receiver.type as Type.List).param, toExpression(expression.receiver))

            FileFunctions.toString -> fileToString(toExpression(expression.receiver))
            FileFunctions.readText -> {
                StaticMethodCall(files, "readString", pathToStringMethod, listOf(toExpression(expression.receiver)))
            }

            else -> throw IllegalArgumentException("Unknown ${expression.target.displayName} for ${expression.receiver.type}")
        }
    }

    private fun concatString(left: JvmExpression, right: JvmExpression): JvmExpression {
        return VirtualMethodCall(TypeDescriptor.String, "concat", stringToStringMethod, left, listOf(right))
    }

    private fun CodeBuilder.callMethod(methodCall: InstanceMethodCall, slots: Map<String, LocalVariable>) {
        push(methodCall.target)
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

            is StaticMethodCall -> {
                expression.args.forEach { push(it, slots) }
                invokeStatic(expression.owner, expression.name, expression.descriptor)
            }

            is VirtualMethodCall -> {
                push(expression.receiver, slots)
                expression.args.forEach { push(it, slots) }
                invokeVirtual(expression.owner, expression.name, expression.descriptor)
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

    private fun CodeBuilder.push(expression: ReferenceExpression) {
        when (expression) {
            is StaticField -> getStaticField(expression.owner, expression.name, expression.type)
        }
    }
}