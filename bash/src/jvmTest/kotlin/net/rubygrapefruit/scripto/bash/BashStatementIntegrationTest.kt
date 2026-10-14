package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.test.AbstractStatementIntegrationTest

class BashStatementIntegrationTest : AbstractStatementIntegrationTest() {
    override fun fixture() = BashFixture(workDir)
}