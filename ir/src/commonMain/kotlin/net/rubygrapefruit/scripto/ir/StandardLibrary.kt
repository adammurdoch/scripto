package net.rubygrapefruit.scripto.ir

interface Functions {
    fun definitions(name: String): List<FunctionDefinition>
}

object StringFunctions : Functions {
    val toString = TypedFunctionDefinition("toString", Type.String, emptyList())

    override fun definitions(name: String): List<FunctionDefinition> {
        return if (name == "toString") {
            listOf(toString)
        } else {
            emptyList()
        }
    }
}

object IntFunctions : Functions {
    val toString = TypedFunctionDefinition("toString", Type.String, emptyList())

    override fun definitions(name: String): List<FunctionDefinition> {
        return if (name == "toString") {
            listOf(toString)
        } else {
            emptyList()
        }
    }
}

object BooleanFunctions : Functions {
    val toString = TypedFunctionDefinition("toString", Type.String, emptyList())

    override fun definitions(name: String): List<FunctionDefinition> {
        return if (name == "toString") {
            listOf(toString)
        } else {
            emptyList()
        }
    }
}

object StandardLibrary : Functions {
    val printlnNoArgs = UnitFunctionDefinition("println", emptyList())
    val printlnOneArg = UnitFunctionDefinition("println", listOf(IncomingType.Any))
    val fileConstructor = TypedFunctionDefinition("File", Type.String, listOf(Type.String))
    val readText = TypedFunctionDefinition("readText", Type.String, listOf(Type.String))
    val intAddition = OperatorDefinition(Operator.Plus, Type.Int, listOf(Type.Int, Type.Int))
    val stringConcatenation = OperatorDefinition(Operator.Plus, Type.String, listOf(Type.String, Type.String))
    val stringEquality = OperatorDefinition(Operator.Equality, Type.Boolean, listOf(Type.String, Type.String))
    val intEquality = OperatorDefinition(Operator.Equality, Type.Boolean, listOf(Type.Int, Type.Int))
    val booleanEquality = OperatorDefinition(Operator.Equality, Type.Boolean, listOf(Type.Boolean, Type.Boolean))

    override fun definitions(name: String): List<FunctionDefinition> {
        return when (name) {
            printlnNoArgs.name -> listOf(printlnNoArgs, printlnOneArg)
            readText.name -> listOf(readText)
            fileConstructor.name -> listOf(fileConstructor)
            else -> emptyList()
        }
    }

    fun definitions(receiver: Type, name: String): List<FunctionDefinition> {
        val functions = when (receiver) {
            is Type.String -> StringFunctions
            is Type.Int -> IntFunctions
            is Type.Boolean -> BooleanFunctions
        }
        return functions.definitions(name)
    }

    fun operator(name: String): List<OperatorDefinition> {
        return when (name) {
            "+" -> listOf(intAddition, stringConcatenation)
            "==" -> listOf(intEquality, booleanEquality, stringEquality)
            else -> emptyList()
        }
    }
}