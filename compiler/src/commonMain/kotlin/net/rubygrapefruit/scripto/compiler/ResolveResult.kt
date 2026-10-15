package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.parse.Position

internal sealed interface ResolveResult<out T> {
    sealed interface Failed<T> : ResolveResult<T> {
        val problems: List<Problem>
    }
}

internal class Resolved<T>(val result: T) : ResolveResult<T>

internal class PartiallyResolved<T>(val result: T, override val problems: List<Problem>) : ResolveResult.Failed<T>

internal class NotResolved(override val problems: List<Problem>) : ResolveResult.Failed<Nothing> {
    companion object {
        fun of(problems: List<String>) = NotResolved(problems.map { Problem(null, it) })
    }
}

class Problem(val position: Position?, val message: String)
