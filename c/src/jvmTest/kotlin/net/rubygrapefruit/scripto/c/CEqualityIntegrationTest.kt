package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.test.AbstractEqualityIntegrationTest

class CEqualityIntegrationTest : AbstractEqualityIntegrationTest() {
    override fun fixture() = CFixture(workDir)
}