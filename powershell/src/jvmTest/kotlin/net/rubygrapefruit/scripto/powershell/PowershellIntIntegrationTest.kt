package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.test.AbstractIntIntegrationTest

class PowershellIntIntegrationTest : AbstractIntIntegrationTest() {
    override fun fixture() = PowershellFixture(workDir)
}