package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.test.AbstractIntAdditionIntegrationTest

class CIntAdditionIntegrationTest : AbstractIntAdditionIntegrationTest() {
    override fun fixture() = CFixture(workDir)
}