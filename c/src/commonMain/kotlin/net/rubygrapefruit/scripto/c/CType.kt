package net.rubygrapefruit.scripto.c

internal sealed interface CType {
    val declaration: String

    fun array(): Array = Array(this, 1)

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

    class Struct(val type: TypeSnippet) : CType {
        override val declaration = "struct ${type.name}"

        override fun visitDeclarations(visitor: (CDeclaration) -> Unit) {
            visitor(type)
        }
    }

    class Array(val element: CType, val dimensions: Int) : CType {
        override val declaration: String
            get() = element.declaration

        override fun array(): Array {
            return Array(element, dimensions + 1)
        }
    }
}