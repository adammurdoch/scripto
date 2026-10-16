package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.test.AbstractIntIntegrationTest

class BashIntIntegrationTest : AbstractIntIntegrationTest() {
    override fun fixture() = BashFixture(workDir)
}