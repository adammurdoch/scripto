package net.rubygrapefruit.scripto.ir

sealed interface OutgoingType {
    data object Void : OutgoingType
}

sealed interface IncomingType {
    data object Any : IncomingType
}

sealed class Type : OutgoingType, IncomingType {
    data object String : Type()
    data object Int : Type()
    data object Boolean : Type()
}