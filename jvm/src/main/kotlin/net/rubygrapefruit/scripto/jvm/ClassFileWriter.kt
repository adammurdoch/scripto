package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.RegularFile

internal class ClassFileWriter {
    fun write(className: String, builder: ClassBuilder.() -> Unit): ByteArray {
        val constantPool = ConstantPool()

        val thisClass = constantPool.addClassInfo(BinaryName.of(className))
        val objectClass = constantPool.addClassInfo(TypeDescriptor.Object.binaryName)
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
    val descriptor: String

    interface ReferenceType : TypeDescriptor

    class ObjectType(val binaryName: BinaryName, override val descriptor: String, val isInterface: kotlin.Boolean) : ReferenceType {
        fun arrayType(): ArrayType {
            return ArrayType("[$descriptor")
        }

        override fun hashCode(): kotlin.Int {
            return binaryName.hashCode()
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

    class ArrayType(override val descriptor: String) : ReferenceType {
    }

    object Int : TypeDescriptor {
        override val descriptor: String
            get() = "I"
    }

    object Boolean : TypeDescriptor {
        override val descriptor: String
            get() = "Z"
    }

    companion object {
        val Object = ofClass("java.lang.Object")

        fun ofClass(className: String): ObjectType {
            val binaryName = BinaryName.of(className)
            return ObjectType(binaryName, "L${binaryName.name};", false)
        }

        fun ofInteface(className: String): ObjectType {
            val binaryName = BinaryName.of(className)
            return ObjectType(binaryName, "L${binaryName.name};", true)
        }
    }
}

internal data class MethodDescriptor(val descriptor: String, val parameters: Int, val stackChange: Int) {
    companion object {
        fun ofVoid(vararg parameters: TypeDescriptor): MethodDescriptor {
            return of("V", parameters.size, *parameters)
        }

        fun of(returnType: TypeDescriptor, vararg parameters: TypeDescriptor): MethodDescriptor {
            return of(returnType.descriptor, parameters.size - 1, *parameters)
        }

        private fun of(returnDescriptor: String, stackChange: Int, vararg parameters: TypeDescriptor): MethodDescriptor {
            val builder = StringBuilder()
            builder.append("(")
            for (descriptor in parameters) {
                builder.append(descriptor.descriptor)
            }
            builder.append(")")
            builder.append(returnDescriptor)
            return MethodDescriptor(builder.toString(), parameters.size, stackChange)
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
    private val maxStack: Int,
    private val maxLocals: Int,
    private val code: ByteArray
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
        val codeEncoder = Encoder()
        // max stack, max locals
        codeEncoder.u2(maxStack)
        codeEncoder.u2(maxLocals)
        // code
        codeEncoder.u4(code.size)
        codeEncoder.bytes(code)
        // exceptions
        codeEncoder.u2(0)
        // attributes
        codeEncoder.u2(0)

        val codeAttribute = codeEncoder.toByteArray()
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
        val descriptorEntry = constantPool.addUtf8(descriptor.descriptor)

        val codeBuilder = DefaultCodeBuilder(descriptor.parameters, constantPool)
        codeBuilder.builder()

        val method = MethodInfo(
            nameEntry,
            descriptorEntry,
            flags,
            codeBuilder.maxStack,
            codeBuilder.maxLocals,
            codeBuilder.encoder.toByteArray()
        )
        methods.add(method)
    }
}

@JvmInline
value class BinaryName private constructor(val name: String) {
    companion object {
        fun of(name: String) = BinaryName(name.replace('.', '/'))
    }
}

internal class LocalVariable(val slot: Int, val type: TypeDescriptor)

internal interface CodeBuilder {
    fun allocateLocal(type: TypeDescriptor): LocalVariable

    fun storeLocal(variable: LocalVariable)

    fun loadLocal(variable: LocalVariable)

    fun loadConstant(value: String)

    fun loadConstant(value: Int)

    fun loadConstant(value: Boolean)

    // Should handle any type
    fun newArray(type: TypeDescriptor.ObjectType)

    fun iadd()

    fun return_()

    // Pushes true or false on the stack
    // Should be generalized to take true and false branches
    fun intEquality()

    fun getStaticField(owner: TypeDescriptor.ObjectType, name: String, type: TypeDescriptor)

    fun invokeVirtual(owner: TypeDescriptor.ObjectType, name: String, type: MethodDescriptor)

    fun invokeStatic(owner: TypeDescriptor.ObjectType, name: String, type: MethodDescriptor)

}

private class DefaultCodeBuilder(params: Int, private val constantPool: ConstantPool) : CodeBuilder {
    val encoder = Encoder()

    var maxLocals = params
        private set

    var maxStack = 0
        private set

    private var currentStack = 0

    override fun allocateLocal(type: TypeDescriptor): LocalVariable {
        return LocalVariable(maxLocals++, type)
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
                push()
            }

            is TypeDescriptor.ReferenceType -> {
                when (variable.slot) {
                    in 0..3 -> encoder.u1(0x2a + variable.slot) // aload_0 .. aload_3
                    else -> {
                        encoder.u1(0x19) // aload
                        encoder.u1(variable.slot)
                    }
                }
                push()
            }
        }
    }

    override fun loadConstant(value: String) {
        val constant = constantPool.addStringInfo(value)
        loadConstant(constant)
    }

    override fun loadConstant(value: Int) {
        when (value) {
            -1 -> {
                encoder.u1(0x2) // iconst_m1
                push()
            }

            in 0..5 -> {
                encoder.u1(0x3 + value) // iconst_0 .. iconst_5
                push()
            }

            else -> {
                val constant = constantPool.addIntegerInfo(value)
                loadConstant(constant)
            }
        }
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
        push()
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
        encoder.u1(0x3) // iconst_0

        pop()
    }

    override fun newArray(type: TypeDescriptor.ObjectType) {
        val classInfo = constantPool.addClassInfo(type.binaryName)

        encoder.u1(0xbd) // anewarray
        encoder.u2(classInfo.index)
    }

    override fun getStaticField(owner: TypeDescriptor.ObjectType, name: String, type: TypeDescriptor) {
        val fieldRef = constantPool.addFieldRef(owner.binaryName, name, type)
        encoder.u1(0xb2) // getstatic
        encoder.u2(fieldRef.index)
        push()
    }

    override fun invokeVirtual(owner: TypeDescriptor.ObjectType, name: String, type: MethodDescriptor) {
        val methodRef = constantPool.addMethodRef(owner.binaryName, name, type)

        encoder.u1(0xb6) // invokevirtual
        encoder.u2(methodRef.index)
        pop(type.stackChange + 1)
    }

    override fun invokeStatic(owner: TypeDescriptor.ObjectType, name: String, type: MethodDescriptor) {
        val methodRef = if (owner.isInterface) {
            constantPool.addInterfaceMethodRef(owner.binaryName, name, type)
        } else {
            constantPool.addMethodRef(owner.binaryName, name, type)
        }

        encoder.u1(0xb8) // invokestatic
        encoder.u2(methodRef.index)
        pop(type.stackChange)
    }

    override fun return_() {
        encoder.u1(0xb1) // return
    }

    private fun push() {
        currentStack++
        if (currentStack > maxStack) {
            maxStack = currentStack
        }
    }

    private fun pop(count: Int = 1) {
        currentStack -= count
    }
}

private class ConstantPool {
    private val entries = mutableListOf<Entry>()
    private val strings = mutableMapOf<String, Entry.Utf8>()
    private val classes = mutableMapOf<BinaryName, Entry.ClassInfo>()
    private val constStrings = mutableMapOf<String, Entry.StringInfo>()
    private val constInts = mutableMapOf<Int, Entry.IntegerInfo>()
    private val fields = mutableMapOf<FieldRef, Entry.FieldRef>()

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
                val bytes = text.toByteArray()
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

    private data class FieldRef(val owner: BinaryName, val name: String, val type: TypeDescriptor)

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

    fun addClassInfo(className: BinaryName): Entry.ClassInfo {
        val current = classes[className]
        return if (current != null) {
            current
        } else {
            val entry = Entry.ClassInfo(addUtf8(className.name), nextIndex())
            entries.add(entry)
            classes[className] = entry
            entry
        }
    }

    fun addFieldRef(owner: BinaryName, name: String, type: TypeDescriptor): Entry.FieldRef {
        val ref = FieldRef(owner, name, type)
        val current = fields[ref]
        return if (current != null) {
            current
        } else {
            val ownerInfo = addClassInfo(owner)
            val nameAndType = addNameAndType(name, type.descriptor)
            val entry = Entry.FieldRef(ownerInfo, nameAndType, nextIndex())
            entries.add(entry)
            fields[ref] = entry
            entry
        }
    }

    fun addMethodRef(owner: BinaryName, name: String, type: MethodDescriptor): Entry.MethodRef {
        val ownerInfo = addClassInfo(owner)
        val nameAndType = addNameAndType(name, type.descriptor)
        val entry = Entry.MethodRef(ownerInfo, nameAndType, nextIndex())
        entries.add(entry)
        return entry
    }

    fun addInterfaceMethodRef(owner: BinaryName, name: String, type: MethodDescriptor): Entry.InterfaceMethodRef {
        val ownerInfo = addClassInfo(owner)
        val nameAndType = addNameAndType(name, type.descriptor)
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