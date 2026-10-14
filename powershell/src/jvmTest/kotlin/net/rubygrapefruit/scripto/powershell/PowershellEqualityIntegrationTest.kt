package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.test.AbstractEqualityIntegrationTest

class PowershellEqualityIntegrationTest : AbstractEqualityIntegrationTest() {
    override fun fixture() = PowershellFixture(workDir)
}