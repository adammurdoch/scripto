package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.RegularFile
import net.rubygrapefruit.scripto.generator.OperationMapper
import net.rubygrapefruit.scripto.ir.*
import java.io.PrintStream
import java.lang.classfile.CodeBuilder
import java.lang.classfile.TypeKind
import java.lang.constant.ClassDesc
import java.lang.constant.MethodTypeDesc

class JvmGenerator {
    private val string = TypeDescriptor.of("java.lang.String")

    private val int = ClassDesc.ofDescriptor("I")
    private val bool = ClassDesc.ofDescriptor("Z")
    private val objectType = ClassDesc.of("java.lang.Object")
    private val integer = ClassDesc.of("java.lang.Integer")
    private val boolean = ClassDesc.of("java.lang.Boolean")
    private val stringX = ClassDesc.of("java.lang.String")
    private val files = ClassDesc.of("java.nio.file.Files")
    private val path = ClassDesc.of("java.nio.file.Path")
    private val intToStringMethod = MethodTypeDesc.of(stringX, int)
    private val boolToStringMethod = MethodTypeDesc.of(stringX, bool)
    private val objectToBoolMethod = MethodTypeDesc.of(bool, objectType)
    private val pathToStringMethod = MethodTypeDesc.of(stringX, path)
    private val stringToPathMethod = MethodTypeDesc.of(path, stringX, stringX.arrayType())
    private val stringToStringMethod = MethodTypeDesc.of(stringX, stringX)

    fun generate(program: Program): GeneratedByteCode {
        val byteCode = ClassFileWriter().write("App") { generate(program) }
        return GeneratedByteCode("App", byteCode)
    }

    fun generate(program: Program, output: RegularFile): GeneratedClass {
        ClassFileWriter().writeTo("App", output) { generate(program) }
        return GeneratedClass("App")
    }

    private fun ClassBuilder.generate(program: Program) {
        val printStream = ClassDesc.of(PrintStream::class.java.name)
        val system = ClassDesc.of(System::class.java.name)

        val stringArray = string.arrayType()

        val void = ClassDesc.ofDescriptor("V")
        val voidMethod = MethodTypeDesc.of(void)
        val stringToVoidMethod = MethodTypeDesc.of(void, stringX)

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

        method("main", MethodDescriptor.ofVoid(stringArray), MethodFlags.PublicStatic) { methodBuilder ->
            methodBuilder.withCode { codeBuilder ->
                val slots = mutableMapOf<String, Int>()
                for (declaration in jvmProgram.variables) {
                    val slot = codeBuilder.allocateLocal(declaration.typeKind)
                    slots[declaration.name] = slot
                }
                for (operation in jvmProgram.operations) {
                    when (operation) {
                        is JvmVariableDeclaration -> {
                            codeBuilder.push(operation.value, slots)
                            codeBuilder.storeLocal(operation.typeKind, slots.getValue(operation.name))
                        }

                        is InstanceMethodCall -> {
                            codeBuilder.callMethod(operation, slots)
                        }
                    }
                }
                codeBuilder.return_()
            }
        }
    }

    private fun toTypeKind(type: Type): TypeKind {
        return when (type) {
            is Type.Boolean -> TypeKind.BOOLEAN
            is Type.Int -> TypeKind.INT
            is Type.String -> TypeKind.REFERENCE
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
            val asPath = StaticMethodCall(path, "of", stringToPathMethod, listOf(toExpression(expression.args.first()), EmptyStringArray), true)
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
            StandardLibrary.stringConcatenation -> VirtualMethodCall(stringX, "concat", stringToStringMethod, toExpression(expression.left), listOf(toExpression(expression.right)))
            StandardLibrary.stringEquality -> JvmStringEquality(toExpression(expression.left), toExpression(expression.right))
            StandardLibrary.intEquality -> JvmIntEquality(toExpression(expression.left), toExpression(expression.right))
            StandardLibrary.booleanEquality -> JvmIntEquality(toExpression(expression.left), toExpression(expression.right))
            else -> throw IllegalArgumentException("Unknown operator '${expression.target.operator.operator}'")
        }
    }

    private fun CodeBuilder.callMethod(methodCall: InstanceMethodCall, slots: Map<String, Int>) {
        push(methodCall.target)
        for (expression in methodCall.arguments) {
            push(expression, slots)
        }
        invokevirtual(methodCall.owner, methodCall.name, methodCall.descriptor)
    }

    private fun CodeBuilder.push(expression: JvmExpression, slots: Map<String, Int>) {
        when (expression) {
            is JvmString -> {
                loadConstant(constantPool().stringEntry(expression.value).constantValue())
            }

            is JvmInt -> {
                loadConstant(constantPool().intEntry(expression.value).constantValue())
            }

            is JvmTrue -> {
                iconst_1()
            }

            is JvmFalse -> {
                iconst_0()
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
                iconst_1()
                goto_(end)
                labelBinding(notEqual)
                iconst_0()
                labelBinding(end)
            }

            is JvmStringEquality -> {
                push(expression.left, slots)
                push(expression.right, slots)
                invokevirtual(objectType, "equals", objectToBoolMethod)
            }

            is JvmVariableReference -> {
                val type = toTypeKind(expression.type)
                loadLocal(type, slots.getValue(expression.name))
            }

            is StaticMethodCall -> {
                expression.args.forEach { push(it, slots) }
                invokestatic(expression.owner, expression.name, expression.descriptor, expression.isInterface)
            }

            is VirtualMethodCall -> {
                push(expression.receiver, slots)
                expression.args.forEach { push(it, slots) }
                invokevirtual(expression.owner, expression.name, expression.descriptor)
            }

            is EmptyStringArray -> {
                iconst_0()
                anewarray(stringX)
            }
        }
    }

    private fun CodeBuilder.push(expression: ReferenceExpression) {
        when (expression) {
            is StaticField -> getstatic(expression.owner, expression.name, expression.type)
        }
    }
}