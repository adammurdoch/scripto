package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.scripto.test.AbstractStringConcatenationIntegrationTest

class JvmStringConcatenationIntegrationTest : AbstractStringConcatenationIntegrationTest() {
    override fun fixture() = JvmFixture(workDir)
}