package net.rubygrapefruit.scripto.test

import kotlin.test.Test

abstract class AbstractBooleanListIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `compiles variable initialized with literal of boolean literals`() {
        val sourceFile = workDir.file("app.scripto")
        sourceFile.writeText(
            """
val l = [true, false]
println(l)
        """
        )
        compileAndRun(sourceFile) {
            output("[true, false]\n")
        }
    }
}