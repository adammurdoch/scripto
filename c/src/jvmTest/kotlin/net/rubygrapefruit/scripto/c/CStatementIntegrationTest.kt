package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.test.AbstractStatementIntegrationTest

class CStatementIntegrationTest : AbstractStatementIntegrationTest() {
    override fun fixture() = CFixture(workDir)
}