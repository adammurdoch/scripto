package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.scripto.test.AbstractStringIntegrationTest

class JvmStringIntegrationTest : AbstractStringIntegrationTest() {
    override fun fixture() = JvmFixture(workDir)
}