package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.test.AbstractIoIntegrationTest

class BashIoIntegrationTest : AbstractIoIntegrationTest() {
    override fun fixture() = BashFixture(workDir)
}