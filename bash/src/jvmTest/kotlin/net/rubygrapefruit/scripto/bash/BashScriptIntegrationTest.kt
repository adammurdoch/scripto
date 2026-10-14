package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.test.AbstractScriptIntegrationTest

class BashScriptIntegrationTest : AbstractScriptIntegrationTest() {
    override fun fixture() = BashFixture(workDir)
}