package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.test.AbstractIntIntegrationTest

class CIntIntegrationTest : AbstractIntIntegrationTest() {
    override fun fixture() = CFixture(workDir)
}