package net.rubygrapefruit.scripto.c

class CStatementScope {
    private var counter = 1

    fun newVarName(base: String): String {
        return "$base${counter++}"
    }
}