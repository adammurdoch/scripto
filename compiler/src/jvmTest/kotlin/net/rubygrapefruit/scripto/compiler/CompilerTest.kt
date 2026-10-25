package net.rubygrapefruit.scripto.compiler

import de.infix.testBalloon.framework.core.testSuite

val CompilerTest by testSuite {
    withCompiler {
        test("fails with syntax error") {
            sourceFile.writeText(
                """
text!
        """.trimIndent()
            )

            compileFails("""1 | text!
        ^
Expected "(", "A".."Z", "_", "a".."z", a digit""")
        }


        test("reports multiple failures") {
            sourceFile.writeText(
                """
unknown1()
val x = println("a", b)
        """.trimIndent()
            )

            compileFails(listOf(
                "Line 1: 1: Unknown function unknown1()",
                "Line 2: 9: Incorrect number of parameters for function println()",
                "Line 2: 22: Unknown variable 'b'"
            ))
        }
    }
}

