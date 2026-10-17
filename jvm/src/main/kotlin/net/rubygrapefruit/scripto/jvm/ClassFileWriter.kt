package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.RegularFile
import java.lang.constant.ClassDesc
import java.lang.constant.MethodTypeDesc

internal class ClassFileWriter {
    fun write(className: String, builder: ClassBuilder.() -> Unit): ByteArray {
        val constantPool = ConstantPool()

        val thisClass = constantPool.addClassInfo(className)
        val objectClass = constantPool.addClassInfo("java.lang.Object")
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

    class ReferenceType(override val descriptor: String) : TypeDescriptor {
        fun arrayType(): ReferenceType {
            return ReferenceType("[$descriptor")
        }
    }

    companion object {
        fun of(className: String): ReferenceType {
            return ReferenceType("L${className.replace('.', '/')};")
        }
    }
}

internal class MethodDescriptor(val descriptor: String, val parameters: Int) {
    companion object {
        fun ofVoid(vararg parameters: TypeDescriptor): MethodDescriptor {
            val builder = StringBuilder()
            builder.append("(")
            for (descriptor in parameters) {
                builder.append(descriptor.descriptor)
            }
            builder.append(")V")
            return MethodDescriptor(builder.toString(), parameters.size)
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
internal value class LocalVariable(val slot: Int)

internal class Label

internal interface CodeBuilder {
    fun allocateLocal(): LocalVariable

    fun storeLocal(variable: LocalVariable)

    fun loadLocal(variable: LocalVariable)

    fun loadConstant(value: String)

    fun loadConstant(value: Int)

    fun anewarray(type: TypeDescriptor)

    fun iadd()

    fun return_()

    fun newLabel(): Label

    fun if_icmpne(target: Label)

    fun goto_(target: Label)

    // REWORK THESE

    fun labelBinding(label: Label)

    fun iconst_1()

    fun iconst_0()

    fun getstatic(owner: ClassDesc, name: String, type: ClassDesc)

    fun invokevirtual(owner: ClassDesc, name: String, type: MethodTypeDesc)

    fun invokestatic(owner: ClassDesc, name: String, type: MethodTypeDesc, isInterface: Boolean)
}

private class DefaultCodeBuilder(params: Int, private val constantPool: ConstantPool) : CodeBuilder {
    val encoder = Encoder()

    var maxLocals = params
        private set

    var maxStack = 0
        private set

    override fun allocateLocal(): LocalVariable {
        return LocalVariable(maxLocals++)
    }

    override fun storeLocal(variable: LocalVariable) {
    }

    override fun loadLocal(variable: LocalVariable) {
    }

    override fun loadConstant(value: String) {
        val constant = constantPool.addStringInfo(value)
        loadConstant(constant)
    }

    override fun loadConstant(value: Int) {
        when (value) {
            -1 -> {
                encoder.u1(0x2)
                maxStack++
            }
            in 0..5 -> {
                encoder.u1(0x3 + value)
                maxStack++
            }
            else -> {
                val constant = constantPool.addIntegerInfo(value)
                loadConstant(constant)
            }
        }
    }

    private fun loadConstant(constant: ConstantPool.Entry.Loadable) {
        encoder.u1(0x12) // ldc
        encoder.u1(constant.index)
        maxStack++
    }

    override fun iconst_0() {
    }

    override fun iconst_1() {
    }

    override fun iadd() {
    }

    override fun newLabel(): Label {
        return Label()
    }

    override fun labelBinding(label: Label) {
    }

    override fun if_icmpne(target: Label) {
    }

    override fun goto_(target: Label) {
    }

    override fun anewarray(type: TypeDescriptor) {
    }

    override fun getstatic(owner: ClassDesc, name: String, type: ClassDesc) {
    }

    override fun invokevirtual(owner: ClassDesc, name: String, type: MethodTypeDesc) {
    }

    override fun invokestatic(owner: ClassDesc, name: String, type: MethodTypeDesc, isInterface: Boolean) {
    }

    override fun return_() {
        encoder.u1(0xb1)
    }
}

private class ConstantPool {
    private val entries = mutableListOf<Entry>()
    private val strings = mutableMapOf<String, Entry.Utf8>()
    private val classes = mutableMapOf<String, Entry.ClassInfo>()
    private val constStrings = mutableMapOf<String, Entry.StringInfo>()
    private val constInts = mutableMapOf<Int, Entry.IntegerInfo>()

    sealed interface Entry {
        fun appendTo(encoder: Encoder)

        sealed interface Loadable : Entry {
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
    }

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

    fun addClassInfo(className: String): Entry.ClassInfo {
        val current = classes[className]
        return if (current != null) {
            current
        } else {
            val entry = Entry.ClassInfo(addUtf8(className.replace('.', '/')), nextIndex())
            entries.add(entry)
            classes[className] = entry
            entry
        }
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