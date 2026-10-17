package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.ir.*

class JvmGenerator {
    private val string = TypeDescriptor.ofClass("java.lang.String")

    private val integer = TypeDescriptor.ofClass("java.lang.Integer")
    private val boolean = TypeDescriptor.ofClass("java.lang.Boolean")
    private val files = TypeDescriptor.ofClass("java.nio.file.Files")
    private val path = TypeDescriptor.ofInteface("java.nio.file.Path")
    private val intToStringMethod = MethodDescriptor.of(string, TypeDescriptor.Int)
    private val boolToStringMethod = MethodDescriptor.of(string, TypeDescriptor.Boolean)
    private val objectToBoolMethod = MethodDescriptor.of(TypeDescriptor.Boolean, TypeDescriptor.Object)
    private val pathToStringMethod = MethodDescriptor.of(string, path)
    private val stringToPathMethod = MethodDescriptor.of(path, string, string.arrayType())
    private val stringToStringMethod = MethodDescriptor.of(string, string)

    fun generate(program: Program): GeneratedByteCode {
        val byteCode = ClassFileWriter().write("App") { generate(program) }
        return GeneratedByteCode("App", byteCode)
    }

    fun generate(program: Program, output: RegularFile): GeneratedClass {
        ClassFileWriter().writeTo("App", output) { generate(program) }
        return GeneratedClass("App")
    }

    private fun ClassBuilder.generate(program: Program) {
        val printStream = TypeDescriptor.ofClass("java.io.PrintStream")
        val system = TypeDescriptor.ofClass("java.lang.System")

        val stringArray = string.arrayType()

        val voidMethod = MethodDescriptor.ofVoid()
        val stringToVoidMethod = MethodDescriptor.ofVoid(string)

        val systemOut = StaticField(system, "out", printStream)

        val mapping = OperationMapper.of {
            mapFun(StandardLibrary.printlnNoArgs) {
                InstanceMethodCall(printStream, "println", voidMethod, systemOut, emptyList())
            }
            mapFun(StandardLibrary.printlnOneArg) {
                val args = it.arguments.map { toStringExpression(it) }
                InstanceMethodCall(printStream, "println", stringToVoidMethod, systemOut, args)
            }
            mapVar { declaration ->
                val type = toTypeKind(declaration.type)
                JvmVariableDeclaration(declaration, type, toExpression(declaration.value))
            }
        }

        val jvmProgram = JvmProgram(mapping.map(program.operations))

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

    private fun toTypeKind(type: Type): TypeDescriptor {
        return when (type) {
            is Type.Boolean -> TypeDescriptor.Boolean
            is Type.Int -> TypeDescriptor.Int
            is Type.String -> string
        }
    }

    private fun toStringExpression(expression: Expression): JvmExpression {
        return when (expression) {
            is StringLiteral -> JvmString(expression.value)
            is IntLiteral -> JvmString(expression.value.toString())
            is BooleanLiteral -> JvmString(expression.value.toString())
            is VariableReference -> convertToString(expression, JvmVariableReference(expression))
            is InfixFunctionCall -> convertToString(expression, mapInfix(expression))
            is FunctionCallExpression -> convertToString(expression, mapFunctionCall(expression))
            is ReceiverFunctionCallExpression -> convertToString(expression, mapFunctionCall(expression))
        }
    }

    private fun convertToString(expression: Expression, jvmExpression: JvmExpression): JvmExpression {
        return when (expression.type) {
            is Type.Int -> intToString(jvmExpression)
            is Type.Boolean -> booleanToString(jvmExpression)
            is Type.String -> jvmExpression
        }
    }

    private fun intToString(expression: JvmExpression): JvmExpression {
        return StaticMethodCall(integer, "toString", intToStringMethod, listOf(expression))
    }

    private fun booleanToString(expression: JvmExpression): JvmExpression {
        return StaticMethodCall(boolean, "toString", boolToStringMethod, listOf(expression))
    }

    private fun toExpression(expression: Expression): JvmExpression {
        return when (expression) {
            is StringLiteral -> JvmString(expression.value)
            is IntLiteral -> JvmInt(expression.value)
            is BooleanLiteral -> if (expression.value) JvmTrue else JvmFalse
            is VariableReference -> JvmVariableReference(expression)
            is InfixFunctionCall -> mapInfix(expression)
            is FunctionCallExpression -> mapFunctionCall(expression)
            is ReceiverFunctionCallExpression -> mapFunctionCall(expression)
        }
    }

    private fun mapFunctionCall(expression: FunctionCallExpression): JvmExpression {
        return if (expression.target == StandardLibrary.readText) {
            val asPath = StaticMethodCall(path, "of", stringToPathMethod, listOf(toExpression(expression.args.first()), EmptyStringArray))
            StaticMethodCall(files, "readString", pathToStringMethod, listOf(asPath))
        } else if (expression.target == StandardLibrary.fileConstructor) {
            toExpression(expression.args.first())
        } else {
            throw IllegalArgumentException("Unknown function '${expression.target.name}'")
        }
    }

    private fun mapFunctionCall(expression: ReceiverFunctionCallExpression): JvmExpression {
        return if (expression.target == StringFunctions.toString) {
            toExpression(expression.receiver)
        } else if (expression.target == IntFunctions.toString) {
            val receiver = expression.receiver
            when (receiver) {
                is IntLiteral -> JvmString(receiver.value.toString())
                else -> intToString(toExpression(receiver))
            }
        } else {
            throw IllegalArgumentException("Unknown function '${expression.target.name}'")
        }
    }

    private fun mapInfix(expression: InfixFunctionCall): JvmExpression {
        return when (expression.target) {
            StandardLibrary.intAddition -> JvmIntAddition(toExpression(expression.left), toExpression(expression.right))
            StandardLibrary.stringConcatenation -> VirtualMethodCall(string, "concat", stringToStringMethod, toExpression(expression.left), listOf(toExpression(expression.right)))
            StandardLibrary.stringEquality -> JvmStringEquality(toExpression(expression.left), toExpression(expression.right))
            StandardLibrary.intEquality -> JvmIntEquality(toExpression(expression.left), toExpression(expression.right))
            StandardLibrary.booleanEquality -> JvmIntEquality(toExpression(expression.left), toExpression(expression.right))
            else -> throw IllegalArgumentException("Unknown operator '${expression.target.operator.operator}'")
        }
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
                val notEqual = newLabel()
                val end = newLabel()
                if_icmpne(notEqual)
                loadConstant(true)
                goto_(end)
                labelBinding(notEqual)
                loadConstant(false)
                labelBinding(end)
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
                newArray(string)
            }
        }
    }

    private fun CodeBuilder.push(expression: ReferenceExpression) {
        when (expression) {
            is StaticField -> getStaticField(expression.owner, expression.name, expression.type)
        }
    }
}