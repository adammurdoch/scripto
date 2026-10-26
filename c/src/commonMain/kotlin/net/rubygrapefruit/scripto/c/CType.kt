package net.rubygrapefruit.scripto.c

internal sealed interface CType {
    val declaration: String

    fun variableDeclaration(name: String): String {
        return "$declaration $name"
    }

    fun array(): Array = Array(this, "[]")

    fun pointer(): Pointer = Pointer(this)

    /**
     * Visits any declarations used directly by this type. May include this type.
     */
    fun visitDeclarations(visitor: (CDeclaration) -> Unit) {
    }

    object CString : CType {
        override val declaration: String
            get() = "const char*"
    }

    object CInt : CType {
        override val declaration: String
            get() = "int"
    }

    object Int32 : CType {
        override val declaration: String
            get() = "int32_t"

        override fun visitDeclarations(visitor: (CDeclaration) -> Unit) {
            visitor(Header.Stdint)
        }
    }

    object Bool : CType {
        override val declaration: String
            get() = "bool"

        override fun visitDeclarations(visitor: (CDeclaration) -> Unit) {
            visitor(Header.Stdbool)
        }
    }

    object CVoidPointer: CType {
        override val declaration: String
            get() = "void*"
    }

    class Pointer(val element: CType) : CType {
        override val declaration: String = "${element.declaration}*"

        override fun visitDeclarations(visitor: (CDeclaration) -> Unit) {
            element.visitDeclarations(visitor)
        }
    }

    class Array(val element: CType, private val dimensions: String) : CType {
        override val declaration: String = element.declaration + dimensions

        override fun variableDeclaration(name: String): String {
            return "${element.declaration} $name$dimensions"
        }

        override fun visitDeclarations(visitor: (CDeclaration) -> Unit) {
            element.visitDeclarations(visitor)
        }

        override fun array(): Array {
            return Array(element, "$dimensions[]")
        }
    }
}