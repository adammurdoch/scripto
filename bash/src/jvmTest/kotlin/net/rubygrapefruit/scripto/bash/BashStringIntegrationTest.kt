package net.rubygrapefruit.scripto.bash

import net.rubygrapefruit.scripto.test.AbstractStringIntegrationTest

class BashStringIntegrationTest : AbstractStringIntegrationTest() {
    override fun fixture() = BashFixture(workDir)
}