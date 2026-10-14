package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.scripto.test.AbstractStdoutIoIntegrationTest

class JvmStdoutIoIntegrationTest : AbstractStdoutIoIntegrationTest() {
    override fun fixture() = JvmFixture(workDir)
}