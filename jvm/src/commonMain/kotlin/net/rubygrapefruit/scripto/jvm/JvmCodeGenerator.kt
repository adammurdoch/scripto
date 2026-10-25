package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.Directory
import net.rubygrapefruit.file.RegularFile

internal fun generate(type: JvmGeneratedType, methods: List<JvmGeneratedMethod>, output: Directory): RegularFile {
    val classFile = output.file("${type.name}.class")
    val needsConstructor = type is JvmGeneratedClass && methods.any { !it.isStatic }
    val isFinal = type is JvmGeneratedClass && type.isFinal
    val interfaces = type.interfaces.map { it.descriptor }

    ClassFileWriter().writeTo(type.descriptor, type.visibility, isFinal, interfaces, classFile) {
        if (needsConstructor) {
            constructor(MethodVisibility.Public)
        }
        for (method in methods) {
            generate(method)
        }
    }
    return classFile
}

private fun ClassBuilder.generate(method: JvmGeneratedMethod) {
    if (method.operations.isEmpty()) {
        method(method.name, method.descriptor, method.visibility)
    } else {
        method(method.name, method.descriptor, method.visibility, method.isStatic) {
            val slots = mutableMapOf<String, LocalVariable>()
            method.parameterNames.forEachIndexed { index, param ->
                slots[param] = parameter(index, method.descriptor.parameters[index])
            }
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

        is JvmIncrementVariable -> {
            incrementLocal(slots.getValue(operation.name))
        }

        is VirtualMethodCall -> callMethod(operation, slots)

        is IfIntsEqual -> {
            push(operation.left, slots)
            push(operation.right, slots)
            intsEqual {
                for (nested in operation.whenTrue) {
                    generate(nested, slots)
                }
            }
        }

        is IfIntGreaterThan -> {
            push(operation.left, slots)
            push(operation.right, slots)
            intGreaterThan {
                for (nested in operation.whenTrue) {
                    generate(nested, slots)
                }
            }
        }

        is WhileIntLessThan -> {
            intWhileLessThan(
                { push(operation.left, slots) },
                { push(operation.right, slots) },
                {
                    for (operation in operation.loop) {
                        generate(operation, slots)
                    }
                }
            )
        }

        is JvmDiscard -> {
            generate(operation.value, slots)
            popStack()
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
            intsEqual({
                loadConstant(true)
            }, {
                loadConstant(false)
            })
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
