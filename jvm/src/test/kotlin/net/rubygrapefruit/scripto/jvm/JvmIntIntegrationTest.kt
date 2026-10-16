package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.scripto.test.AbstractIntIntegrationTest

class JvmIntIntegrationTest : AbstractIntIntegrationTest() {
    override fun fixture() = JvmFixture(workDir)
}