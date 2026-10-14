package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.test.AbstractStdoutIoIntegrationTest

class CStdoutIoIntegrationTest : AbstractStdoutIoIntegrationTest() {
    override fun fixture() = CFixture(workDir)
}