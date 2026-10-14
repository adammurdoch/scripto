package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.test.AbstractFileIoIntegrationTest

class CFileIoIntegrationTest : AbstractFileIoIntegrationTest() {
    override fun fixture() = CFixture(workDir)
}