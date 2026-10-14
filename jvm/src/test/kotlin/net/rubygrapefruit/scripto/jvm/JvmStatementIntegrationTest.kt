package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.scripto.test.AbstractStatementIntegrationTest

class JvmStatementIntegrationTest : AbstractStatementIntegrationTest() {
    override fun fixture() = JvmFixture(workDir)
}