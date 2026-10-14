package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.scripto.test.AbstractScriptIntegrationTest

class JvmScriptIntegrationTest : AbstractScriptIntegrationTest() {
    override fun fixture() = JvmFixture(workDir)
}