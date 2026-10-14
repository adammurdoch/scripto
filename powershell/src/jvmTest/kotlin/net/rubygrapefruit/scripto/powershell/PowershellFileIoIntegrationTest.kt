package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.test.AbstractFileIoIntegrationTest

class PowershellFileIoIntegrationTest : AbstractFileIoIntegrationTest() {
    override fun fixture() = PowershellFixture(workDir)
}