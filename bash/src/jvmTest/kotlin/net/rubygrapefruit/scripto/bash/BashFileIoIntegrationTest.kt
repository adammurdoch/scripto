package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.test.AbstractFileIoIntegrationTest

class BashFileIoIntegrationTest : AbstractFileIoIntegrationTest() {
    override fun fixture() = BashFixture(workDir)
}