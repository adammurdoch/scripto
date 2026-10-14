package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractEqualityIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles string equality argument to stdlib function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println("abc" == "abc")
println("abc" == "123")
println("abc" == "a")
println("abc" == "aBc")
        """
        )

        compileAndRun(sourceFile) {
            statements(4)
            output("true\nfalse\nfalse\nfalse\n")
        }
    }

    @Test
    fun `compiles variable declaration with string equality initializer`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = "abc" == "abc"
val b = "abc" == "123"
println(a)
println(b)
        """
        )

        compileAndRun(sourceFile) {
            statements(4)
            output("true\nfalse\n")
        }
    }

    @Test
    fun `compiles string equality with variable references as arguments`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = "abc"
val b = "abc"
val c = "A"
println(a == b)
println(a == c)
        """
        )

        compileAndRun(sourceFile) {
            statements(5)
            output("true\nfalse\n")
        }
    }

    @Test
    fun `compiles string equality with calculated strings as arguments`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = "abc"
val b = "ab" + "c"
println(a == b)
        """
        )

        compileAndRun(sourceFile) {
            statements(3)
            output("true\n")
        }
    }

    @Test
    fun `compiles int equality argument to stdlib function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(1 == 1)
println(1 == 2)
        """
        )

        compileAndRun(sourceFile) {
            statements(2)
            output("true\nfalse\n")
        }
    }

    @Test
    fun `compiles variable declaration with int equality initializer`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = 123 == 123
val b = 7 == 5
println(a)
println(b)
        """
        )

        compileAndRun(sourceFile) {
            statements(4)
            output("true\nfalse\n")
        }
    }

    @Test
    fun `compiles boolean equality argument to stdlib function`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
println(true == true)
println(false == false)
println(false == true)
        """
        )

        compileAndRun(sourceFile) {
            statements(3)
            output("true\ntrue\nfalse\n")
        }
    }

    @Test
    fun `compiles variable declaration with boolean equality initializer`() {
        val sourceFile = workDir.file("app.scripto")

        sourceFile.writeText(
            """
val a = false == false
val b = true == false
println(a)
println(b)
        """
        )

        compileAndRun(sourceFile) {
            statements(4)
            output("true\nfalse\n")
        }
    }

}