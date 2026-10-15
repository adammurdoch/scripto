package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.test.AbstractStringIntegrationTest

class PowershellStringIntegrationTest : AbstractStringIntegrationTest() {
    override fun fixture() = PowershellFixture(workDir)
}