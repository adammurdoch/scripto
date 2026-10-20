package net.rubygrapefruit.scripto.compiler

import net.rubygrapefruit.parse.Position

internal sealed interface ResolveResult<out T> {
    // May contain duplicates
    val problems: List<Problem>
}

internal class Resolved<T>(val result: T) : ResolveResult<T> {
    override val problems: List<Problem>
        get() = emptyList()
}

internal class NotResolved(override val problems: List<Problem>) : ResolveResult<Nothing> {
    companion object {
        fun of(position: Position, message: String) = NotResolved(listOf(Problem(position, message)))

        fun of(message: String) = NotResolved(listOf(Problem(null, message)))

        fun of(problems: List<String>) = NotResolved(problems.map { Problem(null, it) })
    }
}

internal class Problem(val position: Position?, val message: String)

internal fun <S, T> List<T>.resolving(map: (T) -> ResolveResult<S>): ResolveResult<List<S>> {
    if (isEmpty()) {
        return Resolved(emptyList())
    }

    var problems: MutableList<Problem>? = null
    val result = mutableListOf<S>()
    for (item in this) {
        val itemResult = map(item)
        when (itemResult) {
            is NotResolved -> {
                if (problems == null) {
                    problems = mutableListOf()
                }
                problems.addAll(itemResult.problems)
            }

            is Resolved -> result.add(itemResult.result)
        }
    }
    return if (problems != null) {
        NotResolved(problems)
    } else {
        Resolved(result)
    }
}

internal fun <S, T, U> zipping(s: ResolveResult<S>, t: ResolveResult<T>, map: (S, T) -> ResolveResult<U>): ResolveResult<U> {
    return if (s is Resolved && t is Resolved) {
        map(s.result, t.result)
    } else {
        NotResolved(s.problems + t.problems)
    }
}

internal fun <S, T, U, V> zipping(s: ResolveResult<S>, t: ResolveResult<T>, u: ResolveResult<U>, map: (S, T, U) -> ResolveResult<V>): ResolveResult<V> {
    return if (s is Resolved && t is Resolved && u is Resolved) {
        map(s.result, t.result, u.result)
    } else {
        NotResolved(s.problems + t.problems + u.problems)
    }
}

internal fun <S, T> ResolveResult<T>.mapping(map: (T) -> ResolveResult<S>): ResolveResult<S> {
    return when (this) {
        is NotResolved -> this
        is Resolved -> map(result)
    }
}