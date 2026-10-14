package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.test.AbstractScriptIntegrationTest

class CScriptIntegrationTest : AbstractScriptIntegrationTest() {
    override fun fixture() = CFixture(workDir)
}