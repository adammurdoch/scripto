package net.rubygrapefruit.scripto.ir

interface Functions {
    fun definitions(name: String): List<FunctionDefinition>
}

open class ObjectFunctions : Functions {
    val toString = TypedFunctionDefinition("toString", Type.String, emptyList())

    override fun definitions(name: String): List<FunctionDefinition> {
        return if (name == "toString") {
            listOf(toString)
        } else {
            emptyList()
        }
    }
}

object StringFunctions : ObjectFunctions()

object IntFunctions : ObjectFunctions()

object BooleanFunctions : ObjectFunctions()

object FileFunctions : ObjectFunctions() {
    val readText = TypedFunctionDefinition("readText", Type.String, emptyList())

    override fun definitions(name: String): List<FunctionDefinition> {
        return if (name == "readText") {
            listOf(readText)
        } else {
            super.definitions(name)
        }
    }
}

object StandardLibrary : Functions {
    val printlnNoArgs = UnitFunctionDefinition("println", emptyList())
    val printlnOneArg = UnitFunctionDefinition("println", listOf(IncomingType.Any))
    val fileConstructor = TypedFunctionDefinition("File", Type.File, listOf(Type.String))
    val intAddition = OperatorDefinition(Operator.Plus, Type.Int, listOf(Type.Int, Type.Int))
    val stringConcatenation = OperatorDefinition(Operator.Plus, Type.String, listOf(Type.String, Type.String))
    val stringEquality = OperatorDefinition(Operator.Equality, Type.Boolean, listOf(Type.String, Type.String))
    val intEquality = OperatorDefinition(Operator.Equality, Type.Boolean, listOf(Type.Int, Type.Int))
    val booleanEquality = OperatorDefinition(Operator.Equality, Type.Boolean, listOf(Type.Boolean, Type.Boolean))

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
        }
    }

    override fun definitions(name: String): List<FunctionDefinition> {
        return when (name) {
            printlnNoArgs.name -> listOf(printlnNoArgs, printlnOneArg)
            fileConstructor.name -> listOf(fileConstructor)
            else -> emptyList()
        }
    }

    fun definitions(receiver: Type, name: String): List<FunctionDefinition> {
        val functions = when (receiver) {
            is Type.String -> StringFunctions
            is Type.Int -> IntFunctions
            is Type.Boolean -> BooleanFunctions
            is Type.File -> FileFunctions
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