package net.rubygraperuit.scripto.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
abstract class PowerShellPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            val powerShellInstall = tasks.register("powershell", PowerShellInstall::class.java) {
                it.installDirectory.set(layout.buildDirectory.dir("powershell"))
            }

            afterEvaluate {
                tasks.named("jvmTest") {
                    it.dependsOn(powerShellInstall)
                }
            }
        }
    }
}