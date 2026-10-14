package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.test.AbstractIntAdditionIntegrationTest

class BashIntAdditionIntegrationTest : AbstractIntAdditionIntegrationTest() {
    override fun fixture() = BashFixture(workDir)
}