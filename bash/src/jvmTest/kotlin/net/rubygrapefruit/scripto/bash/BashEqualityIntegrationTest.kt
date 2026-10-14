package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.test.AbstractEqualityIntegrationTest

class BashEqualityIntegrationTest : AbstractEqualityIntegrationTest() {
    override fun fixture() = BashFixture(workDir)
}