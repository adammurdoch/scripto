package net.rubygraperuit.scripto.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import kotlin.jvm.java

@Suppress("unused")
abstract class TestSuitePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            val generateTests = tasks.register("generateTests", GenerateTests::class.java) {
                it.baseName.set(name.capitalized())
                it.sourceDirectory.set(project(":test-suite").projectDir.resolve("src/jvmMain/kotlin"))
                it.outputDirectory.set(layout.buildDirectory.dir("generated/tests"))
            }

            afterEvaluate {
                extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets.getByName("jvmTest").generatedKotlin.srcDir(generateTests.flatMap { it.outputDirectory })
            }
        }
    }
}