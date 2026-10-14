package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.test.AbstractStdoutIoIntegrationTest

class BashStdoutIoIntegrationTest : AbstractStdoutIoIntegrationTest() {
    override fun fixture() = BashFixture(workDir)
}