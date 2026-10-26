package net.rubygrapefruit.scripto.ir

sealed interface Kind

/**
 * A fully realized type with all parameters provided.
 */
sealed interface Type : Kind {
    data object Any : Type
    data object String : Type
    data object Int : Type
    data object Boolean : Type
    data object File : Type

    data class List(val param: Type) : Type {
        override fun toString(): kotlin.String {
            return "List<$param>"
        }
    }
}

/**
 * A type that produces another type from a parameter.
 */
sealed interface TypeFunction : Kind {
    fun evaluate(arg: Type): Type

    object List : TypeFunction {
        override fun evaluate(arg: Type): Type {
            return Type.List(arg)
        }
    }
}