package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.test.AbstractStringConcatenationIntegrationTest

class BashStringConcatenationIntegrationTest: AbstractStringConcatenationIntegrationTest() {
    override fun fixture() = BashFixture(workDir)
}