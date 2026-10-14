package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.test.AbstractIntAdditionIntegrationTest

class PowershellIntAdditionIntegrationTest : AbstractIntAdditionIntegrationTest() {
    override fun fixture() = PowershellFixture(workDir)
}