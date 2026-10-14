package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.test.AbstractIoIntegrationTest

class PowershellIoIntegrationTest : AbstractIoIntegrationTest() {
    override fun fixture() = PowershellFixture(workDir)
}