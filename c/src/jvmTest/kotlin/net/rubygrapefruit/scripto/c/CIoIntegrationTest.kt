package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.test.AbstractIoIntegrationTest

class CIoIntegrationTest : AbstractIoIntegrationTest() {
    override fun fixture() = CFixture(workDir)
}