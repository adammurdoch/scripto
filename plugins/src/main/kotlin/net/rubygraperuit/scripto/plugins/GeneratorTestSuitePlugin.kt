package net.rubygraperuit.scripto.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@OptIn(ExperimentalKotlinGradlePluginApi::class)
@Suppress("unused")
abstract class GeneratorTestSuitePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            val generateTests = tasks.register("generateTests", GenerateTests::class.java) {
                it.baseName.set(name.capitalized())
                it.sourceDirectory.set(project(":test-suite").projectDir.resolve("src/jvmMain/kotlin"))
                it.outputDirectory.set(layout.buildDirectory.dir("generated/tests"))
            }

            afterEvaluate {
                val kotlin = extensions.findByType(KotlinMultiplatformExtension::class.java)
                val sourceSet = if (kotlin != null) {
                    kotlin.sourceSets.getByName("jvmTest")
                } else {
                    val jvm = extensions.getByType(KotlinJvmProjectExtension::class.java)
                    jvm.sourceSets.getByName("test")
                }
                sourceSet.generatedKotlin.srcDir(generateTests.flatMap { it.outputDirectory })
            }
        }
    }
}