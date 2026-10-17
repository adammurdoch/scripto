package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.file.RegularFile
import java.lang.classfile.MethodBuilder

internal class ClassFileWriter {
    fun write(className: String, builder: ClassBuilder.() -> Unit): ByteArray {
        val constantPool = ConstantPool()

        val thisClass = constantPool.addClassInfo(className)
        val objectClass = constantPool.addClassInfo("java.lang.Object")

        DefaultClassBuilder().builder()

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
        encoder.u2(0)

        // attributes
        encoder.u2(0)

        return encoder.toByteArray()
    }

    fun writeTo(className: String, output: RegularFile, builder: ClassBuilder.() -> Unit) {
        val bytecode = write(className, builder)
        output.writeBytes(bytecode)
    }
}

internal sealed class TypeDescriptor {
    class ReferenceType : TypeDescriptor() {
        fun arrayType(): ReferenceType {
            return ReferenceType()
        }
    }

    companion object {
        fun of(className: String): ReferenceType {
            return ReferenceType()
        }
    }
}

internal class MethodDescriptor {
    companion object {
        fun ofVoid(vararg parameters: TypeDescriptor): MethodDescriptor {
            return MethodDescriptor()
        }
    }
}

internal class MethodFlags {
    companion object {
        val PublicStatic = MethodFlags()
    }
}

internal interface ClassBuilder {

    fun method(name: String, descriptor: MethodDescriptor, flags: MethodFlags, builder: (MethodBuilder) -> Unit)
}

private class DefaultClassBuilder : ClassBuilder {
    override fun method(name: String, descriptor: MethodDescriptor, flags: MethodFlags, builder: MethodBuilder.() -> Unit) {
    }
}

private class ConstantPool {
    private val entries = mutableListOf<Entry>()
    private val strings = mutableMapOf<String, Entry.Utf8>()
    private val classes = mutableMapOf<String, Entry.ClassInfo>()

    sealed interface Entry {
        fun appendTo(encoder: Encoder)

        class Utf8(val text: String, val index: Int) : Entry {
            override fun appendTo(encoder: Encoder) {
                encoder.u1(1)
                val bytes = text.toByteArray()
                encoder.u2(bytes.size)
                encoder.bytes(bytes)
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
            val entry = Entry.Utf8(text, entries.size + 1)
            entries.add(entry)
            strings[text] = entry
            entry
        }
    }

    fun addClassInfo(className: String): Entry.ClassInfo {
        val current = classes[className]
        return if (current != null) {
            current
        } else {
            val entry = Entry.ClassInfo(addUtf8(className.replace('.', '/')), entries.size + 1)
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
}

private class Encoder {
    private val bytes = ByteArray(4096)
    private var pos = 0

    fun u4(value: Long) {
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