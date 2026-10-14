package net.rubygrapefruit.scripto.c

import net.rubygrapefruit.scripto.test.AbstractStringConcatenationIntegrationTest

class CStringConcatenationIntegrationTest : AbstractStringConcatenationIntegrationTest() {
    override fun fixture() = CFixture(workDir)
}