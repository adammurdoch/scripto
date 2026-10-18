package net.rubygrapefruit.scripto.ir

sealed interface IncomingType {
    data object Any : IncomingType
}

sealed class Type : IncomingType {
    data object String : Type()
    data object Int : Type()
    data object Boolean : Type()
}