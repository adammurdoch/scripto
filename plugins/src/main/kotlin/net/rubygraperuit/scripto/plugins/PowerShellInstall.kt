package net.rubygraperuit.scripto.plugins

import net.rubygrapefruit.download.DownloadRepository
import net.rubygrapefruit.machine.info.Machine
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
abstract class PowerShellInstall : DefaultTask() {
    @get:OutputDirectory
    abstract val installDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val url = when (Machine.thisMachine) {
            is Machine.MacOSArm64 -> "https://github.com/PowerShell/PowerShell/releases/download/v7.6.8/powershell-7.6.8-osx-arm64.tar.gz"
            is Machine.LinuxX64 -> "https://github.com/PowerShell/PowerShell/releases/download/v7.6.8/powershell-7.6.8-linux-x64.tar.gz"
            is Machine.WindowsX64 -> "https://github.com/PowerShell/PowerShell/releases/download/v7.6.8/PowerShell-7.6.8-win-x64.zip"
            else -> throw IllegalStateException("Unsupported host machine: ${Machine.thisMachine}")
        }
        val baseName = url.substringAfterLast("/").removeSuffix(".tar.gz").removeSuffix(".zip")
        val repository = DownloadRepository()
        val downloadDir = repository.install(URI(url), baseName)
        val installDir = installDirectory.get().asFile.toPath()
        installDir.deleteRecursively()
        downloadDir.copyToRecursively(installDir, followLinks = false, overwrite = false)
    }
}