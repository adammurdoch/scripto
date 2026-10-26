package net.rubygraperuit.scripto.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
abstract class NodeJsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            val nodeJsInstall = tasks.register("nodeJs", NodeJsInstall::class.java) {
                it.installDirectory.set(layout.buildDirectory.dir("node-js"))
            }

            afterEvaluate {
                tasks.named("jvmTest") {
                    it.dependsOn(nodeJsInstall)
                }
            }
        }
    }
}