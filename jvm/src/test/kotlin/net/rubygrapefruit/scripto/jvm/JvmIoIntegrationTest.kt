package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.scripto.test.AbstractIoIntegrationTest

class JvmIoIntegrationTest : AbstractIoIntegrationTest() {
    override fun fixture() = JvmFixture(workDir)
}