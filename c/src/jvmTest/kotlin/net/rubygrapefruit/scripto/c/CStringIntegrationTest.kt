package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.test.AbstractStringIntegrationTest

class CStringIntegrationTest : AbstractStringIntegrationTest() {
    override fun fixture() = CFixture(workDir)
}