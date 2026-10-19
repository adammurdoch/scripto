package net.rubygrapefruit.scripto.ir

sealed interface Type {
    data object Any : Type
    data object String : Type
    data object Int : Type
    data object Boolean : Type
    data object File : Type

    sealed class List(val param: Type) : Type
}