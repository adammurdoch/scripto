package net.rubygrapefruit.script.interpreter

interface StdOut {
    fun println(text: String)

    object Default : StdOut {
        override fun println(text: String) {
            kotlin.io.println(text)
        }
    }
}