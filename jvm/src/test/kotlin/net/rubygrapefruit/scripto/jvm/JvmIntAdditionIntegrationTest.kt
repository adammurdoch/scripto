package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.scripto.test.AbstractIntAdditionIntegrationTest

class JvmIntAdditionIntegrationTest : AbstractIntAdditionIntegrationTest() {
    override fun fixture() = JvmFixture(workDir)
}