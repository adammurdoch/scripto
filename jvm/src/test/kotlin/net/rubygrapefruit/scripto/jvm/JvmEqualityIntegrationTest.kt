package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.scripto.test.AbstractEqualityIntegrationTest

class JvmEqualityIntegrationTest : AbstractEqualityIntegrationTest() {
    override fun fixture() = JvmFixture(workDir)
}