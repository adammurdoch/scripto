package net.rubygrapefruit.scripto.ir

class StandardLibrary {
    fun definitions(name: String): List<FunctionDefinition> {
        return when (name) {
            printlnNoArgs.name -> listOf(printlnNoArgs, printlnOneArg)
            readText.name -> listOf(readText)
            else -> emptyList()
        }
    }

    fun operator(name: String): List<OperatorDefinition> {
        return if (name == "+") {
            listOf(intAddition, stringConcatenation)
        } else if (name == "==") {
            listOf(intEquality, booleanEquality, stringEquality)
        } else {
            emptyList()
        }
    }

    companion object {
        val printlnNoArgs = VoidFunctionDefinition("println", emptyList())
        val printlnOneArg = VoidFunctionDefinition("println", listOf(IncomingType.Any))
        val readText = TypedFunctionDefinition("readText", Type.String, listOf(Type.String))
        val intAddition = OperatorDefinition(Operator.Plus, Type.Int, listOf(Type.Int, Type.Int))
        val stringConcatenation = OperatorDefinition(Operator.Plus, Type.String, listOf(Type.String, Type.String))
        val stringEquality = OperatorDefinition(Operator.Equality, Type.Boolean, listOf(Type.String, Type.String))
        val intEquality = OperatorDefinition(Operator.Equality, Type.Boolean, listOf(Type.Int, Type.Int))
        val booleanEquality = OperatorDefinition(Operator.Equality, Type.Boolean, listOf(Type.Boolean, Type.Boolean))
    }
}