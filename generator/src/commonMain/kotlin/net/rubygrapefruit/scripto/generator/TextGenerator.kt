package net.rubygrapefruit.scripto.generator

import net.rubygrapefruit.file.RegularFile

fun writeTo(file: RegularFile, builder: SourceGenerator.() -> Unit) {
    val text = StringBuilder()
    DefaultSourceGenerator(text).builder()
    file.writeText(text.toString())
}

interface SourceGenerator {
    fun append(ch: Char)
    fun append(text: String)

    fun appendln(ch: Char)
    fun appendln(text: String)
    fun appendln()

}

inline fun <T> SourceGenerator.append(elements: Iterable<T>, separator: String, builder: SourceGenerator.(T) -> Unit) {
    elements.forEachIndexed { index, t ->
        if (index > 0) {
            append(separator)
        }
        builder(t)
    }
}

private class DefaultSourceGenerator(val builder: StringBuilder) : SourceGenerator {
    override fun append(ch: Char) {
        builder.append(ch)
    }

    override fun append(text: String) {
        builder.append(text)
    }

    override fun appendln(text: String) {
        append(text)
        appendln()
    }

    override fun appendln(ch: Char) {
        append(ch)
        appendln()
    }

    override fun appendln() {
        builder.append("\n")
    }
}
