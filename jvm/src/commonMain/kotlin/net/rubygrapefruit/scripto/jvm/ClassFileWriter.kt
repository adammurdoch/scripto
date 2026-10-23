package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.RegularFile
import kotlin.jvm.JvmInline

internal class ClassFileWriter {
    fun write(type: TypeDescriptor.ObjectType, flags: Int, interfaces: List<TypeDescriptor.InterfaceType>, builder: ClassBuilder.() -> Unit): ByteArray {
        val constantPool = ConstantPool()

        val thisType = constantPool.addClassInfo(type)
        val objectClass = constantPool.addClassInfo(TypeDescriptor.Object)
        val interfaceTypes = interfaces.map { constantPool.addClassInfo(it) }
        val codeAttributeName = constantPool.addUtf8("Code")

        val classBuilder = DefaultClassBuilder(type, constantPool)
        classBuilder.builder()

        val encoder = Encoder()

        // Magic
        encoder.u4(0xCAFEBABE)

        // minor, major (Java 11)
        encoder.u2(0)
        encoder.u2(55)

        // constant pool
        constantPool.appendTo(encoder)

        // flags
        encoder.u2(flags)

        // this, super
        encoder.u2(thisType.index)
        encoder.u2(objectClass.index)

        // interfaces
        encoder.u2(interfaceTypes.size)
        for (interfaceType in interfaceTypes) {
            encoder.u2(interfaceType.index)
        }

        // fields
        encoder.u2(0)

        // methods
        encoder.u2(classBuilder.methods.size)
        for (method in classBuilder.methods) {
            method.appendTo(encoder, codeAttributeName)
        }

        // attributes
        encoder.u2(0)

        return encoder.toByteArray()
    }

    fun writeTo(
        classDescriptor: TypeDescriptor.ObjectType,
        visibility: ClassVisibility,
        isFinal: Boolean,
        interfaces: List<TypeDescriptor.InterfaceType>,
        output: RegularFile,
        builder: ClassBuilder.() -> Unit
    ) {
        val flags = when (classDescriptor) {
            is TypeDescriptor.InterfaceType -> visibility.flags.or(0x600) // interface and abstract flags
            is TypeDescriptor.ClassType -> if (isFinal) visibility.flags.or(0x0010) else visibility.flags
        }
        val bytecode = write(classDescriptor, flags, interfaces, builder)
        output.writeBytes(bytecode)
    }
}

internal sealed interface TypeDescriptor {
    val descriptor: TypeDescriptorString

    fun arrayType(): ArrayType {
        return ArrayType(BinaryName("[${descriptor.value}"), TypeDescriptorString("[${descriptor.value}"), this)
    }

    sealed interface ReferenceType : TypeDescriptor {
        val binaryName: BinaryName
    }

    sealed interface ObjectType : ReferenceType

    class ClassType(override val binaryName: BinaryName, override val descriptor: TypeDescriptorString) : ObjectType {
        override fun hashCode(): kotlin.Int {
            return descriptor.hashCode()
        }

        override fun equals(other: Any?): kotlin.Boolean {
            return if (other === this) {
                true
            } else if (other == null || other !is ClassType) {
                false
            } else {
                binaryName == other.binaryName
            }
        }
    }

    class InterfaceType(override val binaryName: BinaryName, override val descriptor: TypeDescriptorString) : ObjectType {
        override fun hashCode(): kotlin.Int {
            return descriptor.hashCode()
        }

        override fun equals(other: Any?): kotlin.Boolean {
            return if (other === this) {
                true
            } else if (other == null || other !is InterfaceType) {
                false
            } else {
                binaryName == other.binaryName
            }
        }
    }

    class ArrayType(override val binaryName: BinaryName, override val descriptor: TypeDescriptorString, val elementType: TypeDescriptor) : ReferenceType {
        override fun hashCode(): kotlin.Int {
            return descriptor.hashCode()
        }

        override fun equals(other: Any?): kotlin.Boolean {
            return if (other === this) {
                true
            } else if (other == null || other !is ArrayType) {
                false
            } else {
                elementType == other.elementType
            }
        }
    }

    object Int : TypeDescriptor {
        override val descriptor: TypeDescriptorString
            get() = TypeDescriptorString("I")
    }

    object Boolean : TypeDescriptor {
        override val descriptor: TypeDescriptorString
            get() = TypeDescriptorString("Z")
    }

    companion object {
        val Object = ofClass("java.lang.Object")
        val String = ofClass("java.lang.String")

        fun ofClass(className: String): ClassType {
            val binaryName = BinaryName(className.replace('.', '/'))
            return ClassType(binaryName, TypeDescriptorString("L${binaryName.name};"))
        }

        fun ofInterface(className: String): InterfaceType {
            val binaryName = BinaryName(className.replace('.', '/'))
            return InterfaceType(binaryName, TypeDescriptorString("L${binaryName.name};"))
        }
    }
}

internal data class MethodDescriptor(val descriptor: MethodDescriptorString, val returnType: TypeDescriptor?, val parameters: List<TypeDescriptor>) {
    companion object {
        fun ofVoid(vararg parameters: TypeDescriptor): MethodDescriptor {
            return of(TypeDescriptorString("V"), null, parameters.toList())
        }

        fun of(returnType: TypeDescriptor, vararg parameters: TypeDescriptor): MethodDescriptor {
            return of(returnType.descriptor, returnType, parameters.toList())
        }

        private fun of(returnDescriptor: TypeDescriptorString, returnType: TypeDescriptor?, parameters: List<TypeDescriptor>): MethodDescriptor {
            val builder = StringBuilder()
            builder.append("(")
            for (descriptor in parameters) {
                builder.append(descriptor.descriptor.value)
            }
            builder.append(")")
            builder.append(returnDescriptor.value)
            val descriptor = MethodDescriptorString(builder.toString())
            return MethodDescriptor(descriptor, returnType, parameters)
        }
    }
}

internal class ClassVisibility(val flags: Int) {
    companion object {
        val Public = ClassVisibility(1)
        val PackageProtected = ClassVisibility(0)
    }
}

internal class MethodVisibility(val flags: Int) {
    companion object {
        val Private = MethodVisibility(2)
        val Public = MethodVisibility(1)
    }
}


private class MethodInfo(
    private val name: ConstantPool.Entry.Utf8,
    private val descriptor: ConstantPool.Entry.Utf8,
    private val flags: Int,
    private val codeAttribute: ByteArray?
) {
    fun appendTo(encoder: Encoder, codeAttributeName: ConstantPool.Entry.Utf8) {
        // Access flags
        encoder.u2(flags)

        // Name, descriptor
        encoder.u2(name.index)
        encoder.u2(descriptor.index)

        // Attributes
        if (codeAttribute == null) {
            encoder.u2(0)
        } else {
            encoder.u2(1)

            // Code attribute
            encoder.u2(codeAttributeName.index)
            encoder.u4(codeAttribute.size)
            encoder.bytes(codeAttribute)
        }
    }
}

internal interface ClassBuilder {
    /**
     * Adds a default constructor
     */
    fun constructor(visibility: MethodVisibility)

    /**
     * Adds a method.
     */
    fun method(name: String, descriptor: MethodDescriptor, visibility: MethodVisibility, isStatic: Boolean, builder: CodeBuilder.() -> Unit)

    /**
     * Adds an abstract method.
     */
    fun method(name: String, descriptor: MethodDescriptor, visibility: MethodVisibility)
}

private class DefaultClassBuilder(private val thisType: TypeDescriptor.ObjectType, private val constantPool: ConstantPool) : ClassBuilder {
    val methods: List<MethodInfo>
        field = mutableListOf<MethodInfo>()

    override fun constructor(visibility: MethodVisibility) {
        method("<init>", MethodDescriptor.ofVoid(), visibility, false) {
            loadParameter(0, thisType)
            invokeSpecial(TypeDescriptor.Object, "<init>", MethodDescriptor.ofVoid())
            returnVoid()
        }
    }

    override fun method(name: String, descriptor: MethodDescriptor, visibility: MethodVisibility, isStatic: Boolean, builder: CodeBuilder.() -> Unit) {
        val params = if (isStatic) {
            descriptor.parameters
        } else {
            listOf(thisType) + descriptor.parameters
        }
        val codeBuilder = DefaultCodeBuilder(params, constantPool)
        codeBuilder.builder()

        val flags = if (isStatic) visibility.flags.or(8) else visibility.flags

        method(name, descriptor, flags, codeBuilder.codeAttribute())
    }

    override fun method(name: String, descriptor: MethodDescriptor, visibility: MethodVisibility) {
        val flags = visibility.flags.or(0x400) // abstract
        method(name, descriptor, flags, null)
    }

    private fun method(name: String, descriptor: MethodDescriptor, flags: Int, encodedCodeAttribute: ByteArray?) {
        val nameEntry = constantPool.addUtf8(name)
        val descriptorEntry = constantPool.addUtf8(descriptor.descriptor.value)

        val method = MethodInfo(nameEntry, descriptorEntry, flags, encodedCodeAttribute)
        methods.add(method)
    }
}

@JvmInline
value class BinaryName(val name: String)

@JvmInline
value class TypeDescriptorString(val value: String)

@JvmInline
value class MethodDescriptorString(val value: String)

internal class LocalVariable(val slot: Int, val type: TypeDescriptor)

internal interface CodeBuilder {
    fun allocateLocal(type: TypeDescriptor): LocalVariable

    fun storeLocal(variable: LocalVariable)

    fun loadParameter(index: Int, type: TypeDescriptor)

    fun loadLocal(variable: LocalVariable)

    fun loadConstant(value: String)

    fun loadConstant(value: Int)

    fun loadConstant(value: Boolean)

    fun loadNull()

    fun checkCast(type: TypeDescriptor.ReferenceType)

    fun dup()

    fun popStack()

    fun newArray(elementType: TypeDescriptor)

    fun arrayLoad(elementType: TypeDescriptor)

    fun arrayStore(elementType: TypeDescriptor)

    fun arrayLength()

    fun iadd()

    fun returnVoid()

    fun returnValue(type: TypeDescriptor)

    /**
     * Compares 2 ints and runs block if they are equal
     */
    fun intEquality(whenTrue: CodeBuilder.() -> Unit)

    /**
     * Compares 2 ints and runs first block if they are equal or second block if they are not equal
     */
    fun intEquality(whenTrue: CodeBuilder.() -> Unit, whenFalse: CodeBuilder.() -> Unit)

    /**
     * Compares 2 ints `a` and `b` and runs block while `a < b`
     */
    fun intWhileLessThan(loadA: CodeBuilder.() -> Unit, loadB: CodeBuilder.() -> Unit, loop: CodeBuilder.() -> Unit)

    fun getStaticField(owner: TypeDescriptor.ObjectType, name: String, type: TypeDescriptor)

    fun invokeVirtual(owner: TypeDescriptor.ObjectType, name: String, descriptor: MethodDescriptor)

    fun invokeStatic(owner: TypeDescriptor.ObjectType, name: String, descriptor: MethodDescriptor)

    fun invokeSpecial(owner: TypeDescriptor.ClassType, name: String, descriptor: MethodDescriptor)

    fun new(type: TypeDescriptor.ClassType)
}

private class DefaultCodeBuilder(private val params: List<TypeDescriptor>, private val constantPool: ConstantPool) : CodeBuilder {
    private val encoder = Encoder()
    private val stackMap = mutableListOf<StackFrame>()
    private val locals = mutableListOf<TypeDescriptor>()
    private var maxLocals = params.size
    private val stack = mutableListOf<TypeDescriptor>()
    private var maxStack = 0
    private var startFrame = 0

    private class StackFrame(val offset: Int, val locals: List<TypeDescriptor>, val stack: List<TypeDescriptor>)

    init {
        locals.addAll(params)
    }

    fun codeAttribute(): ByteArray {
        val codeEncoder = Encoder()

        // max stack, max locals
        codeEncoder.u2(maxStack)
        codeEncoder.u2(locals.size)

        // byte code
        val code = encoder.toByteArray()
        codeEncoder.u4(code.size)
        codeEncoder.bytes(code)

        // exceptions
        codeEncoder.u2(0)

        // attributes
        codeEncoder.u2(1)

        // stack map table
        val stackMapEncoder = Encoder()
        stackMapEncoder.u2(stackMap.size)
        stackMap.forEachIndexed { index, entry ->
            val previousLocals = if (index == 0) {
                params
            } else {
                stackMap[index - 1].locals
            }
            stackMapEncoder.stackFrame(entry, previousLocals)
        }

        codeEncoder.u2(constantPool.addUtf8("StackMapTable").index)
        val stackMap = stackMapEncoder.toByteArray()
        codeEncoder.u4(stackMap.size)
        codeEncoder.bytes(stackMap)

        return codeEncoder.toByteArray()
    }

    private fun Encoder.stackFrame(frame: StackFrame, previousLocals: List<TypeDescriptor>) {
        if (frame.locals == previousLocals) {
            if (frame.stack.isEmpty() && frame.offset <= 63) {
                // type + offset - same stack frame
                u1(frame.offset)
                return
            }
            if (frame.stack.size == 1 && frame.offset <= 63) {
                // type + offset - same stack frame + 1 stack
                u1(frame.offset + 64)
                verificationType(frame.stack.first())
                return
            }
        }
        // type - full stack frame
        u1(255)

        // offset
        u2(frame.offset)

        // locals
        u2(frame.locals.size)
        for (local in frame.locals) {
            verificationType(local)
        }

        // stack
        u2(frame.stack.size)
        for (stack in frame.stack) {
            verificationType(stack)
        }
    }

    private fun Encoder.verificationType(local: TypeDescriptor) {
        when (local) {
            is TypeDescriptor.Int, is TypeDescriptor.Boolean -> u1(1)
            is TypeDescriptor.ReferenceType -> {
                u1(7)
                u2(constantPool.addClassInfo(local).index)
            }
        }
    }

    override fun allocateLocal(type: TypeDescriptor): LocalVariable {
        val slot = maxLocals
        maxLocals++
        return LocalVariable(slot, type)
    }

    override fun storeLocal(variable: LocalVariable) {
        when (variable.type) {
            is TypeDescriptor.Boolean, is TypeDescriptor.Int -> {
                when (variable.slot) {
                    in 0..3 -> encoder.u1(0x3b + variable.slot) // istore_0 .. istore_3
                    else -> {
                        encoder.u1(0x36) // istore
                        encoder.u1(variable.slot)
                    }
                }
                pop()
            }

            is TypeDescriptor.ReferenceType -> {
                when (variable.slot) {
                    in 0..3 -> encoder.u1(0x4b + variable.slot) // astore_0 .. astore_3
                    else -> {
                        encoder.u1(0x3a) // astore
                        encoder.u1(variable.slot)
                    }
                }
                pop()
            }
        }
        if (locals.size == variable.slot) {
            locals.add(variable.type)
        } else {
            locals[variable.slot] = variable.type
        }
    }

    override fun loadLocal(variable: LocalVariable) {
        loadLocal(variable.slot, variable.type)
    }

    override fun loadParameter(index: Int, type: TypeDescriptor) {
        loadLocal(index, type)
    }

    private fun loadLocal(slot: Int, type: TypeDescriptor) {
        when (type) {
            is TypeDescriptor.Boolean, is TypeDescriptor.Int -> {
                when (slot) {
                    in 0..3 -> encoder.u1(0x1a + slot) // iload_0 .. iload_3
                    else -> {
                        encoder.u1(0x15) // iload
                        encoder.u1(slot)
                    }
                }
            }

            is TypeDescriptor.ReferenceType -> {
                when (slot) {
                    in 0..3 -> encoder.u1(0x2a + slot) // aload_0 .. aload_3
                    else -> {
                        encoder.u1(0x19) // aload
                        encoder.u1(slot)
                    }
                }
            }
        }
        push(type)
    }

    override fun loadConstant(value: String) {
        val constant = constantPool.addStringInfo(value)
        loadConstant(constant)
        push(TypeDescriptor.String)
    }

    override fun loadConstant(value: Int) {
        when (value) {
            -1 -> {
                encoder.u1(0x2) // iconst_m1
            }

            in 0..5 -> {
                encoder.u1(0x3 + value) // iconst_0 .. iconst_5
            }

            else -> {
                val constant = constantPool.addIntegerInfo(value)
                loadConstant(constant)
            }
        }
        push(TypeDescriptor.Int)
    }

    override fun loadConstant(value: Boolean) {
        if (value) {
            loadConstant(1)
        } else {
            loadConstant(0)
        }
    }

    private fun loadConstant(constant: ConstantPool.Entry.Loadable) {
        if (constant.index > 255) {
            // Only a single byte is used for the index, need to use ldc_w for indices > 255
            // Can also change the constant pool so that it inserts constants at the start of the pool
            TODO()
        }

        encoder.u1(0x12) // ldc
        encoder.u1(constant.index)
    }

    override fun loadNull() {
        encoder.u1(0x1) // aconst_null
        push(TypeDescriptor.Object)
    }

    override fun iadd() {
        encoder.u1(0x60) // iadd
        pop()
    }

    override fun intEquality(whenTrue: CodeBuilder.() -> Unit) {
        val startPos = encoder.length
        encoder.u1(0xa0) // if_icmpne

        val endTargetPos = encoder.length
        encoder.u2(0) // target end of true branch
        pop(2)

        val frame = snapshotFrame()

        whenTrue()

        // end of true branch, patch target and start new frame

        val endPos = encoder.length
        encoder.u2(endTargetPos, endPos - startPos)

        startFrame(frame)
    }

    override fun intEquality(whenTrue: CodeBuilder.() -> Unit, whenFalse: CodeBuilder.() -> Unit) {
        val startPos = encoder.length

        encoder.u1(0xa0) // if_icmpne
        val falseBranchTargetPos = encoder.length
        encoder.u2(0) // target false branch
        pop(2)

        val frame = snapshotFrame()

        whenTrue()
        val startTruePos = encoder.length
        encoder.u1(0xa7) // goto
        val endTargetPos = encoder.length
        encoder.u2(0) // end target branch

        // start false branch, patch target and start new frame
        encoder.u2(falseBranchTargetPos, encoder.length - startPos)
        startFrame(frame)
        whenFalse()

        // end of true branch, patch target and start new frame
        encoder.u2(endTargetPos, encoder.length - startTruePos)
        // this reuses the frame from the false branch, but this is not correct if the false branch ends with a return opcode
        startFrame()
    }

    override fun intWhileLessThan(loadA: CodeBuilder.() -> Unit, loadB: CodeBuilder.() -> Unit, loop: CodeBuilder.() -> Unit) {
        val startPos = encoder.length

        startFrame()

        loadA()
        loadB()

        val startCheck = encoder.length
        encoder.u1(0xa2) // if_icmpge
        val endTargetPos = encoder.length
        encoder.u2(0) // target false branch
        pop(2)

        val frame = snapshotFrame()

        loop()

        val endLoopPos = encoder.length
        encoder.u1(0xc8) // goto_w
        encoder.u4(startPos - endLoopPos)

        // end of true branch, patch target and start new frame

        val endPos = encoder.length
        encoder.u2(endTargetPos, endPos - startCheck)

        startFrame(frame)
    }

    override fun checkCast(type: TypeDescriptor.ReferenceType) {
        encoder.u1(0xc0)
        encoder.u2(constantPool.addClassInfo(type).index)
        pop()
        push(type)
    }

    override fun dup() {
        encoder.u1(0x59) // dup
        push(stack.last())
    }

    override fun popStack() {
        encoder.u1(0x57) // pop
        pop()
    }

    override fun arrayLength() {
        encoder.u1(0xbe) // arraylength
        pop()
        push(TypeDescriptor.Int)
    }

    override fun newArray(elementType: TypeDescriptor) {
        when (elementType) {
            is TypeDescriptor.Int -> {
                encoder.u1(0xbc) // newarray
                encoder.u1(10) // int[]
            }

            is TypeDescriptor.Boolean -> {
                encoder.u1(0xbc) // newarray
                encoder.u1(4) // boolean[]
            }

            is TypeDescriptor.ReferenceType -> {
                val classInfo = constantPool.addClassInfo(elementType)

                encoder.u1(0xbd) // anewarray
                encoder.u2(classInfo.index)
            }
        }

        pop()
        push(elementType.arrayType())
    }

    override fun arrayLoad(elementType: TypeDescriptor) {
        when (elementType) {
            is TypeDescriptor.Int -> {
                encoder.u1(0x2e) // iaload
            }

            is TypeDescriptor.Boolean -> {
                encoder.u1(0x33) // baload
            }

            is TypeDescriptor.ReferenceType -> {
                encoder.u1(0x32) // aaload
            }
        }

        pop(2)
        push(elementType)
    }

    override fun arrayStore(elementType: TypeDescriptor) {
        when (elementType) {
            is TypeDescriptor.Int -> {
                encoder.u1(0x4f) // iastore
            }

            is TypeDescriptor.Boolean -> {
                encoder.u1(0x54) // bastore
            }

            is TypeDescriptor.ReferenceType -> {
                encoder.u1(0x53) // aastore
            }
        }
        pop(3)
    }

    override fun getStaticField(owner: TypeDescriptor.ObjectType, name: String, type: TypeDescriptor) {
        val fieldRef = constantPool.addFieldRef(owner, name, type)
        encoder.u1(0xb2) // getstatic
        encoder.u2(fieldRef.index)
        push(type)
    }

    override fun invokeVirtual(owner: TypeDescriptor.ObjectType, name: String, descriptor: MethodDescriptor) {
        when (owner) {
            is TypeDescriptor.InterfaceType -> {
                val methodRef = constantPool.addInterfaceMethodRef(owner, name, descriptor)

                encoder.u1(0xb9) // invokeinterface
                encoder.u2(methodRef.index)
                encoder.u1(descriptor.parameters.size + 1)
                encoder.u1(0)
            }

            is TypeDescriptor.ClassType -> {
                val methodRef = constantPool.addMethodRef(owner, name, descriptor)

                encoder.u1(0xb6) // invokevirtual
                encoder.u2(methodRef.index)
            }
        }

        updateStack(descriptor)
    }

    override fun invokeStatic(owner: TypeDescriptor.ObjectType, name: String, descriptor: MethodDescriptor) {
        val methodRef = when (owner) {
            is TypeDescriptor.InterfaceType -> constantPool.addInterfaceMethodRef(owner, name, descriptor)
            is TypeDescriptor.ClassType -> constantPool.addMethodRef(owner, name, descriptor)
        }

        encoder.u1(0xb8) // invokestatic
        encoder.u2(methodRef.index)
        pop(descriptor.parameters.size)
        if (descriptor.returnType != null) {
            push(descriptor.returnType)
        }
    }

    override fun invokeSpecial(owner: TypeDescriptor.ClassType, name: String, descriptor: MethodDescriptor) {
        val methodRef = constantPool.addMethodRef(owner, name, descriptor)

        encoder.u1(0xb7) // invokespecial
        encoder.u2(methodRef.index)
        updateStack(descriptor)
    }

    private fun updateStack(descriptor: MethodDescriptor) {
        pop(descriptor.parameters.size + 1)
        if (descriptor.returnType != null) {
            push(descriptor.returnType)
        }
    }

    override fun new(type: TypeDescriptor.ClassType) {
        val classInfo = constantPool.addClassInfo(type)

        encoder.u1(0xbb)
        encoder.u2(classInfo.index)

        push(type)
    }

    override fun returnVoid() {
        encoder.u1(0xb1) // return
    }

    override fun returnValue(type: TypeDescriptor) {
        when (type) {
            is TypeDescriptor.Int -> encoder.u1(0xac) // ireturn
            is TypeDescriptor.Boolean -> encoder.u1(0xac) // ireturn
            is TypeDescriptor.ReferenceType -> encoder.u1(0xb0) // areturn
        }
    }

    private fun snapshotFrame(): StackFrame {
        return StackFrame(0, locals.toList(), stack.toList())
    }

    private fun startFrame(snapshot: StackFrame) {
        val offset = if (startFrame == 0) encoder.length else encoder.length - startFrame - 1
        stackMap.add(StackFrame(offset, snapshot.locals, snapshot.stack))
        locals.clear()
        locals.addAll(snapshot.locals)
        stack.clear()
        stack.addAll(snapshot.stack)
        startFrame = encoder.length
    }

    private fun startFrame() {
        val offset = if (startFrame == 0) encoder.length else encoder.length - startFrame - 1
        stackMap.add(StackFrame(offset, locals.toList(), stack.toList()))
        startFrame = encoder.length
    }

    private fun push(type: TypeDescriptor) {
        stack.add(type)
        if (stack.size > maxStack) {
            maxStack = stack.size
        }
    }

    private fun pop(count: Int = 1) {
        repeat(count) {
            stack.removeLast()
        }
    }
}

private class ConstantPool {
    private val entries = mutableListOf<Entry>()
    private val strings = mutableMapOf<String, Entry.Utf8>()
    private val classes = mutableMapOf<BinaryName, Entry.ClassInfo>()
    private val constStrings = mutableMapOf<String, Entry.StringInfo>()
    private val constInts = mutableMapOf<Int, Entry.IntegerInfo>()
    private val fields = mutableMapOf<FieldRef, Entry.FieldRef>()
    private val methods = mutableMapOf<MethodRef, Entry.MethodRef>()

    sealed interface Entry {
        fun appendTo(encoder: Encoder)

        sealed interface Loadable : Entry {
            val index: Int
        }

        sealed interface Ref : Entry {
            val index: Int
        }

        class Utf8(val text: String, val index: Int) : Entry {
            override fun appendTo(encoder: Encoder) {
                encoder.u1(1)
                val bytes = text.encodeToByteArray()
                encoder.u2(bytes.size)
                encoder.bytes(bytes)
            }
        }

        class StringInfo(val value: Utf8, override val index: Int) : Loadable {
            override fun appendTo(encoder: Encoder) {
                encoder.u1(8)
                encoder.u2(value.index)
            }
        }

        class IntegerInfo(val value: Int, override val index: Int) : Loadable {
            override fun appendTo(encoder: Encoder) {
                encoder.u1(3)
                encoder.u4(value)
            }
        }

        class ClassInfo(val name: Utf8, val index: Int) : Entry {
            override fun appendTo(encoder: Encoder) {
                encoder.u1(7)
                encoder.u2(name.index)
            }
        }

        class FieldRef(val owner: ClassInfo, val nameAndType: NameAndType, val index: Int) : Entry {
            override fun appendTo(encoder: Encoder) {
                encoder.u1(9)
                encoder.u2(owner.index)
                encoder.u2(nameAndType.index)
            }
        }

        class MethodRef(val owner: ClassInfo, val nameAndType: NameAndType, override val index: Int) : Ref {
            override fun appendTo(encoder: Encoder) {
                encoder.u1(10)
                encoder.u2(owner.index)
                encoder.u2(nameAndType.index)
            }
        }

        class InterfaceMethodRef(val owner: ClassInfo, val nameAndType: NameAndType, override val index: Int) : Ref {
            override fun appendTo(encoder: Encoder) {
                encoder.u1(11)
                encoder.u2(owner.index)
                encoder.u2(nameAndType.index)
            }
        }

        class NameAndType(val name: Utf8, val descriptor: Utf8, val index: Int) : Entry {
            override fun appendTo(encoder: Encoder) {
                encoder.u1(12)
                encoder.u2(name.index)
                encoder.u2(descriptor.index)
            }
        }
    }

    private data class FieldRef(val owner: TypeDescriptorString, val name: String, val type: TypeDescriptorString)

    private data class MethodRef(val owner: TypeDescriptorString, val name: String, val descriptor: MethodDescriptorString)

    fun addUtf8(text: String): Entry.Utf8 {
        val current = strings[text]
        return if (current != null) {
            current
        } else {
            val entry = Entry.Utf8(text, nextIndex())
            entries.add(entry)
            strings[text] = entry
            entry
        }
    }

    fun addStringInfo(text: String): Entry.StringInfo {
        val current = constStrings[text]
        return if (current != null) {
            current
        } else {
            val entry = Entry.StringInfo(addUtf8(text), nextIndex())
            entries.add(entry)
            constStrings[text] = entry
            entry
        }
    }

    fun addIntegerInfo(value: Int): Entry.IntegerInfo {
        val current = constInts[value]
        return if (current != null) {
            current
        } else {
            val entry = Entry.IntegerInfo(value, nextIndex())
            entries.add(entry)
            constInts[value] = entry
            entry
        }
    }

    fun addClassInfo(type: TypeDescriptor.ReferenceType): Entry.ClassInfo {
        val current = classes[type.binaryName]
        return if (current != null) {
            current
        } else {
            val entry = Entry.ClassInfo(addUtf8(type.binaryName.name), nextIndex())
            entries.add(entry)
            classes[type.binaryName] = entry
            entry
        }
    }

    fun addFieldRef(owner: TypeDescriptor.ObjectType, name: String, type: TypeDescriptor): Entry.FieldRef {
        val ref = FieldRef(owner.descriptor, name, type.descriptor)
        val current = fields[ref]
        return if (current != null) {
            current
        } else {
            val ownerInfo = addClassInfo(owner)
            val nameAndType = addNameAndType(name, type.descriptor.value)
            val entry = Entry.FieldRef(ownerInfo, nameAndType, nextIndex())
            entries.add(entry)
            fields[ref] = entry
            entry
        }
    }

    fun addMethodRef(owner: TypeDescriptor.ClassType, name: String, type: MethodDescriptor): Entry.MethodRef {
        val ref = MethodRef(owner.descriptor, name, type.descriptor)
        val current = methods[ref]
        return if (current != null) {
            current
        } else {
            val ownerInfo = addClassInfo(owner)
            val nameAndType = addNameAndType(name, type.descriptor.value)
            val entry = Entry.MethodRef(ownerInfo, nameAndType, nextIndex())
            entries.add(entry)
            methods[ref] = entry
            entry
        }
    }

    fun addInterfaceMethodRef(owner: TypeDescriptor.InterfaceType, name: String, type: MethodDescriptor): Entry.InterfaceMethodRef {
        val ownerInfo = addClassInfo(owner)
        val nameAndType = addNameAndType(name, type.descriptor.value)
        val entry = Entry.InterfaceMethodRef(ownerInfo, nameAndType, nextIndex())
        entries.add(entry)
        return entry
    }

    private fun addNameAndType(name: String, descriptor: String): Entry.NameAndType {
        val nameEntry = addUtf8(name)
        val descriptorEntry = addUtf8(descriptor)
        val entry = Entry.NameAndType(nameEntry, descriptorEntry, nextIndex())
        entries.add(entry)
        return entry
    }

    fun appendTo(encoder: Encoder) {
        encoder.u2(entries.size + 1)
        for (entry in entries) {
            entry.appendTo(encoder)
        }
    }

    private fun nextIndex(): Int {
        return entries.size + 1
    }
}

private class Encoder {
    private val bytes = ByteArray(4096)
    private var pos = 0

    val length: Int
        get() = pos

    fun u4(value: Long) {
        u4(value.toInt())
    }

    fun u4(value: Int) {
        bytes[pos] = value.shr(24).and(0xFF).toByte()
        bytes[pos + 1] = value.shr(16).and(0xFF).toByte()
        bytes[pos + 2] = value.shr(8).and(0xFF).toByte()
        bytes[pos + 3] = value.and(0xFF).toByte()
        pos += 4
    }

    /**
     * Updates previously encoded u2 that the given position.
     */
    fun u2(pos: Int, value: Int) {
        bytes[pos] = value.shr(8).and(0xFF).toByte()
        bytes[pos + 1] = value.and(0xFF).toByte()
    }

    fun u2(value: Int) {
        u2(pos, value)
        pos += 2
    }

    fun u1(value: Int) {
        bytes[pos] = value.and(0xFF).toByte()
        pos += 1
    }

    fun bytes(from: ByteArray) {
        from.copyInto(bytes, pos)
        pos += from.size
    }

    fun toByteArray(): ByteArray {
        return bytes.sliceArray(0 until pos)
    }
}