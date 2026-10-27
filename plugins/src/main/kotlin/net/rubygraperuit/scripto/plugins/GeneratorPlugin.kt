package net.rubygraperuit.scripto.plugins

import net.rubygrapefruit.plugins.app.MultiPlatformLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
abstract class GeneratorPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            plugins.apply("net.rubygrapefruit.kmp.lib")
            plugins.apply("scripto.test-suite")

            val library = project.extensions.getByType(MultiPlatformLibrary::class.java)
            library.jvm()
            library.nativeDesktop()

            library.test {
                implementation(project(":test-suite"))
            }
        }
    }
}