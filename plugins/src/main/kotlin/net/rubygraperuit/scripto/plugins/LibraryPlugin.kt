package net.rubygraperuit.scripto.plugins

import net.rubygrapefruit.plugins.app.MultiPlatformLibrary
import net.rubygrapefruit.plugins.app.Versions
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
abstract class LibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            plugins.apply("net.rubygrapefruit.kmp.lib")

            val library = project.extensions.getByType(MultiPlatformLibrary::class.java)
            library.jvm()
            library.nativeDesktop()
            library.test {
                implementation(Versions.test.coordinates)
            }
        }
    }
}