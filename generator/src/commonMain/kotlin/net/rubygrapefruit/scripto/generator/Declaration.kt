package net.rubygrapefruit.scripto.generator

/**
 * A declaration of something that references other declarations.
 */
interface Declaration<out SELF : Declaration<SELF>> {
    /**
     * Visits the declarations directly referenced by this declaration.
     */
    fun visitOwnDeclarations(visitor: (SELF) -> Unit)

    class Queue<T : Declaration<T>> {
        private val declarationState = mutableMapOf<T, VisitState>()
        private val declarationQueue = mutableListOf<T>()

        fun add(declaration: T) {
            if (!declarationState.containsKey(declaration)) {
                declarationQueue.add(declaration)
                declarationState[declaration] = VisitState.Queued
            }
        }

        /**
         * Visits queued declarations depth-wise, dependencies before dependents
         */
        fun visitAll(visitor: (T) -> Unit) {
            while (declarationQueue.isNotEmpty()) {
                val declaration = declarationQueue.first()
                val state = declarationState[declaration]
                if (state == null || state == VisitState.Queued) {
                    declaration.visitOwnDeclarations { declarationQueue.add(0, it) }
                    declarationState[declaration] = VisitState.DependenciesQueued
                } else {
                    if (state == VisitState.DependenciesQueued) {
                        visitor(declaration)
                        declarationState[declaration] = VisitState.Finished
                    }
                    declarationQueue.removeFirst()
                }
            }
        }

        fun getAll(): List<T> {
            val declarations = mutableListOf<T>()
            visitAll(declarations::add)
            return declarations
        }

        private enum class VisitState {
            Queued, DependenciesQueued, Finished
        }
    }
}