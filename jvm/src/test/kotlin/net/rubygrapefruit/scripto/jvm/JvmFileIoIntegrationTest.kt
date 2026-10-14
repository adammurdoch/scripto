package net.rubygrapefruit.scripto.jvm

import net.rubygrapefruit.scripto.test.AbstractFileIoIntegrationTest

class JvmFileIoIntegrationTest : AbstractFileIoIntegrationTest() {
    override fun fixture() = JvmFixture(workDir)
}