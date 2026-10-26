package net.rubygraperuit.scripto.plugins

import net.rubygrapefruit.machine.info.Machine

abstract class PowerShellInstall : Download() {
    override fun url(): String {
        return when (Machine.thisMachine) {
            is Machine.MacOSArm64 -> "https://github.com/PowerShell/PowerShell/releases/download/v7.6.8/powershell-7.6.8-osx-arm64.tar.gz"
            is Machine.LinuxX64 -> "https://github.com/PowerShell/PowerShell/releases/download/v7.6.8/powershell-7.6.8-linux-x64.tar.gz"
            is Machine.WindowsX64 -> "https://github.com/PowerShell/PowerShell/releases/download/v7.6.8/PowerShell-7.6.8-win-x64.zip"
            else -> throw IllegalStateException("Unsupported host machine: ${Machine.thisMachine}")
        }
    }
}