package net.rubygraperuit.scripto.plugins

import net.rubygrapefruit.download.DownloadRepository
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
        val repository = DownloadRepository()
        val downloadDir = repository.install(
            URI("https://github.com/PowerShell/PowerShell/releases/download/v7.6.7/powershell-7.6.7-osx-arm64.tar.gz"),
            "powershell-7.6.7-arm64"
        )
        val installDir = installDirectory.get().asFile.toPath()
        installDir.deleteRecursively()
        downloadDir.copyToRecursively(installDir, followLinks = false, overwrite = false)
    }
}