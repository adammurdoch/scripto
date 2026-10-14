package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.test.AbstractStdoutIoIntegrationTest

class PowershellStdoutIoIntegrationTest : AbstractStdoutIoIntegrationTest() {
    override fun fixture() = PowershellFixture(workDir)
}