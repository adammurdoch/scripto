package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.test.AbstractScriptIntegrationTest

class PowershellScriptIntegrationTest : AbstractScriptIntegrationTest() {
    override fun fixture() = PowershellFixture(workDir)
}