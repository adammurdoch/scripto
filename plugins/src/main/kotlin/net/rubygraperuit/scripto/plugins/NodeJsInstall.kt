package net.rubygraperuit.scripto.plugins

import net.rubygrapefruit.machine.info.Machine

abstract class NodeJsInstall : Download() {
    override fun url(): String {
        return when (Machine.thisMachine) {
            is Machine.MacOSArm64 -> "https://nodejs.org/dist/v24.22.1/node-v24.22.1-darwin-arm64.tar.gz"
            is Machine.LinuxX64 -> "https://nodejs.org/dist/v24.22.1/node-v24.22.1-linux-x64.tar.xz"
            is Machine.WindowsX64 -> "https://nodejs.org/dist/v24.22.1/node-v24.22.1-win-x64.zip"
            else -> throw IllegalStateException("Unsupported host machine: ${Machine.thisMachine}")
        }
    }
}