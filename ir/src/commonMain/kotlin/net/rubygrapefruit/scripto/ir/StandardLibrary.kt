package net.rubygrapefruit.scripto.ir

interface Functions {
    fun definitions(name: String): List<FunctionKind>
}

open class ObjectFunctions : Functions {
    val toString = NamedFunctionDefinition("toString", Type.String)

    override fun definitions(name: String): List<FunctionDefinition> {
        return if (name == "toString") {
            listOf(toString)
        } else {
            emptyList()
        }
    }
}

object AnyFunctions : Functions {
    override fun definitions(name: String): List<FunctionKind> {
        return emptyList()
    }
}

object StringFunctions : ObjectFunctions()

object IntFunctions : ObjectFunctions()

object BooleanFunctions : ObjectFunctions()

object FileFunctions : ObjectFunctions() {
    val readText = NamedFunctionDefinition("readText", Type.String, emptyList())

    override fun definitions(name: String): List<FunctionDefinition> {
        return if (name == "readText") {
            listOf(readText)
        } else {
            super.definitions(name)
        }
    }
}

object ListFunctions : ObjectFunctions()

object StandardLibrary : Functions {
    val printlnNoArgs = UnitFunctionDefinition("println")
    val printlnOneArg = UnitFunctionDefinition("println", Type.Any)
    val fileConstructor = NamedFunctionDefinition("File", Type.File, Type.String)
    val intAddition = OperatorDefinition(Operator.Plus, Type.Int, Type.Int, Type.Int)
    val stringConcatenation = OperatorDefinition(Operator.Plus, Type.String, Type.String, Type.String)
    val stringEquality = OperatorDefinition(Operator.Equality, Type.Boolean, Type.String, Type.String)
    val intEquality = OperatorDefinition(Operator.Equality, Type.Boolean, Type.Int, Type.Int)
    val booleanEquality = OperatorDefinition(Operator.Equality, Type.Boolean, Type.Boolean, Type.Boolean)
    val listConstructor = NamedParameterizedFunctionDefinition("List", TypeFunction.List, listOf(ParameterFunction.UseFunctionParameter(true)))

    /**
     * Replaces println(Any) with println(String)
     */
    fun convertPrintln(call: FunctionCall): FunctionCall {
        val arg = call.arguments.first()
        return when (arg.type) {
            is Type.String -> call
            is Type.Boolean -> FunctionCall(printlnOneArg, listOf(ReceiverFunctionCallExpression(arg, BooleanFunctions.toString, emptyList())))
            is Type.Int -> FunctionCall(printlnOneArg, listOf(ReceiverFunctionCallExpression(arg, IntFunctions.toString, emptyList())))
            is Type.File -> FunctionCall(printlnOneArg, listOf(ReceiverFunctionCallExpression(arg, FileFunctions.toString, emptyList())))
            is Type.List -> FunctionCall(printlnOneArg, listOf(ReceiverFunctionCallExpression(arg, ListFunctions.toString, emptyList())))
            else -> throw IllegalArgumentException()
        }
    }

    override fun definitions(name: String): List<FunctionKind> {
        return when (name) {
            printlnNoArgs.name -> listOf(printlnNoArgs, printlnOneArg)
            fileConstructor.name -> listOf(fileConstructor)
            listConstructor.name -> listOf(listConstructor)
            else -> emptyList()
        }
    }

    fun definitions(receiver: Type, name: String): List<FunctionKind> {
        val functions = functionsFor(receiver)
        return functions.definitions(name)
    }

    private fun functionsFor(receiver: Type): Functions {
        return when (receiver) {
            is Type.Any -> AnyFunctions
            is Type.String -> StringFunctions
            is Type.Int -> IntFunctions
            is Type.Boolean -> BooleanFunctions
            is Type.File -> FileFunctions
            is Type.List -> ListFunctions
        }
    }

    fun operator(name: String): List<OperatorDefinition> {
        return when (name) {
            "+" -> listOf(intAddition, stringConcatenation)
            "==" -> listOf(intEquality, booleanEquality, stringEquality)
            else -> emptyList()
        }
    }
}