package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.RegularFile
import kotlin.jvm.JvmInline

internal class ClassFileWriter {
    fun write(className: String, builder: ClassBuilder.() -> Unit): ByteArray {
        val constantPool = ConstantPool()

        val thisClass = constantPool.addClassInfo(TypeDescriptor.ofClass(className))
        val objectClass = constantPool.addClassInfo(TypeDescriptor.Object)
        val codeAttributeName = constantPool.addUtf8("Code")

        val classBuilder = DefaultClassBuilder(constantPool)
        classBuilder.builder()

        val encoder = Encoder()

        // Magic
        encoder.u4(0xCAFEBABE)

        // minor, major (Java 11)
        encoder.u2(0)
        encoder.u2(55)

        // constant pool
        constantPool.appendTo(encoder)

        // flags - public
        encoder.u2(0x1)

        // this, super
        encoder.u2(thisClass.index)
        encoder.u2(objectClass.index)

        // interfaces
        encoder.u2(0)

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

    fun writeTo(className: String, output: RegularFile, builder: ClassBuilder.() -> Unit) {
        val bytecode = write(className, builder)
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

    class ObjectType(override val binaryName: BinaryName, override val descriptor: TypeDescriptorString, val isInterface: kotlin.Boolean) : ReferenceType {

        override fun hashCode(): kotlin.Int {
            return descriptor.hashCode()
        }

        override fun equals(other: Any?): kotlin.Boolean {
            return if (other === this) {
                true
            } else if (other == null || other !is ObjectType) {
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

        fun ofClass(className: String): ObjectType {
            return of(className, false)
        }

        fun ofInterface(className: String): ObjectType {
            return of(className, true)
        }

        private fun of(className: String, isInterface: kotlin.Boolean): ObjectType {
            val binaryName = BinaryName(className.replace('.', '/'))
            return ObjectType(binaryName, TypeDescriptorString("L${binaryName.name};"), isInterface)
        }
    }
}

internal data class MethodDescriptor(val descriptor: MethodDescriptorString, val parameters: List<TypeDescriptor>, val stackChange: Int) {
    companion object {
        fun ofVoid(vararg parameters: TypeDescriptor): MethodDescriptor {
            return of(TypeDescriptorString("V"), parameters.size, parameters.toList())
        }

        fun of(returnType: TypeDescriptor, vararg parameters: TypeDescriptor): MethodDescriptor {
            return of(returnType.descriptor, parameters.size - 1, parameters.toList())
        }

        private fun of(returnDescriptor: TypeDescriptorString, stackChange: Int, parameters: List<TypeDescriptor>): MethodDescriptor {
            val builder = StringBuilder()
            builder.append("(")
            for (descriptor in parameters) {
                builder.append(descriptor.descriptor.value)
            }
            builder.append(")")
            builder.append(returnDescriptor.value)
            val descriptor = MethodDescriptorString(builder.toString())
            return MethodDescriptor(descriptor, parameters, stackChange)
        }
    }
}

internal class MethodFlags(val flags: Int) {
    companion object {
        val PublicStatic = MethodFlags(0x0009)
    }
}

private class MethodInfo(
    private val name: ConstantPool.Entry.Utf8,
    private val descriptor: ConstantPool.Entry.Utf8,
    private val flags: MethodFlags,
    private val codeAttribute: ByteArray
) {
    fun appendTo(encoder: Encoder, codeAttributeName: ConstantPool.Entry.Utf8) {
        // Access flags
        encoder.u2(flags.flags)

        // Name, descriptor
        encoder.u2(name.index)
        encoder.u2(descriptor.index)

        // Attributes
        encoder.u2(1)

        // Code attribute
        encoder.u2(codeAttributeName.index)
        encoder.u4(codeAttribute.size)
        encoder.bytes(codeAttribute)
    }
}

internal interface ClassBuilder {
    fun method(name: String, descriptor: MethodDescriptor, flags: MethodFlags, builder: CodeBuilder.() -> Unit)
}

private class DefaultClassBuilder(private val constantPool: ConstantPool) : ClassBuilder {
    val methods: List<MethodInfo>
        field = mutableListOf<MethodInfo>()

    override fun method(name: String, descriptor: MethodDescriptor, flags: MethodFlags, builder: CodeBuilder.() -> Unit) {
        val nameEntry = constantPool.addUtf8(name)
        val descriptorEntry = constantPool.addUtf8(descriptor.descriptor.value)

        val codeBuilder = DefaultCodeBuilder(descriptor.parameters, constantPool)
        codeBuilder.builder()

        val method = MethodInfo(
            nameEntry,
            descriptorEntry,
            flags,
            codeBuilder.codeAttribute()
        )
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

    fun loadLocal(variable: LocalVariable)

    fun loadConstant(value: String)

    fun loadConstant(value: Int)

    fun loadConstant(value: Boolean)

    fun dup()

    fun newArray(elementType: TypeDescriptor)

    fun arrayStore(elementType: TypeDescriptor)

    fun iadd()

    fun return_()

    // Pushes true or false on the stack
    // Should be generalized to take true and false branches
    fun intEquality()

    fun getStaticField(owner: TypeDescriptor.ObjectType, name: String, type: TypeDescriptor)

    fun invokeVirtual(owner: TypeDescriptor.ObjectType, name: String, type: MethodDescriptor)

    fun invokeStatic(owner: TypeDescriptor.ObjectType, name: String, type: MethodDescriptor)

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
        when (variable.type) {
            is TypeDescriptor.Boolean, is TypeDescriptor.Int -> {
                when (variable.slot) {
                    in 0..3 -> encoder.u1(0x1a + variable.slot) // iload_0 .. iload_3
                    else -> {
                        encoder.u1(0x15) // iload
                        encoder.u1(variable.slot)
                    }
                }
            }

            is TypeDescriptor.ReferenceType -> {
                when (variable.slot) {
                    in 0..3 -> encoder.u1(0x2a + variable.slot) // aload_0 .. aload_3
                    else -> {
                        encoder.u1(0x19) // aload
                        encoder.u1(variable.slot)
                    }
                }
            }
        }
        push(variable.type)
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

    override fun iadd() {
        encoder.u1(0x60) // iadd
        pop()
    }

    override fun intEquality() {
        encoder.u1(0xa0) // if_icmpne
        encoder.u2(7) // target false branch

        // True branch
        encoder.u1(0x4) // iconst_1
        encoder.u1(0xa7) // goto
        encoder.u2(4) // target end

        // False branch
        pop(2)
        startFrame()
        encoder.u1(0x3) // iconst_0

        push(TypeDescriptor.Int)
        startFrame()
    }

    override fun dup() {
        encoder.u1(0x59) // dup
        push(stack.last())
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

    override fun invokeVirtual(owner: TypeDescriptor.ObjectType, name: String, type: MethodDescriptor) {
        val methodRef = constantPool.addMethodRef(owner, name, type)

        encoder.u1(0xb6) // invokevirtual
        encoder.u2(methodRef.index)
        pop(type.stackChange + 1)
    }

    override fun invokeStatic(owner: TypeDescriptor.ObjectType, name: String, type: MethodDescriptor) {
        val methodRef = if (owner.isInterface) {
            constantPool.addInterfaceMethodRef(owner, name, type)
        } else {
            constantPool.addMethodRef(owner, name, type)
        }

        encoder.u1(0xb8) // invokestatic
        encoder.u2(methodRef.index)
        pop(type.stackChange)
    }

    override fun return_() {
        encoder.u1(0xb1) // return
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

    fun addMethodRef(owner: TypeDescriptor.ObjectType, name: String, type: MethodDescriptor): Entry.MethodRef {
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

    fun addInterfaceMethodRef(owner: TypeDescriptor.ObjectType, name: String, type: MethodDescriptor): Entry.InterfaceMethodRef {
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

    fun u2(value: Int) {
        bytes[pos] = value.shr(8).and(0xFF).toByte()
        bytes[pos + 1] = value.and(0xFF).toByte()
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