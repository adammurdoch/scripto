package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.test.AbstractStatementIntegrationTest

class PowershellStatementIntegrationTest : AbstractStatementIntegrationTest() {
    override fun fixture() = PowershellFixture(workDir)
}