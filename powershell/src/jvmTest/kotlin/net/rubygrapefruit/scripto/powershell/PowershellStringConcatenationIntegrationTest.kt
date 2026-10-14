package net.rubygrapefruit.scripto.powershell

import net.rubygrapefruit.scripto.test.AbstractStringConcatenationIntegrationTest

class PowershellStringConcatenationIntegrationTest : AbstractStringConcatenationIntegrationTest() {
    override fun fixture() = PowershellFixture(workDir)
}