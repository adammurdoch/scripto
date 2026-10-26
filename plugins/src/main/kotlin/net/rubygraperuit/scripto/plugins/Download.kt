package net.rubygraperuit.scripto.plugins

import net.rubygrapefruit.download.DownloadRepository
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
abstract class Download : DefaultTask() {
    @get:OutputDirectory
    abstract val installDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val url = url()
        val baseName = url.substringAfterLast("/").removeSuffix(".tar.gz").removeSuffix(".zip")
        val repository = DownloadRepository()
        val downloadDir = repository.install(URI(url), baseName)
        val installDir = installDirectory.get().asFile.toPath()
        installDir.deleteRecursively()
        val directoryEntries = downloadDir.listDirectoryEntries()
        val sourceDir = if (directoryEntries.size == 1 && directoryEntries.first().isDirectory()) {
            directoryEntries.first()
        } else {
            downloadDir
        }

        sourceDir.copyToRecursively(installDir, followLinks = false, overwrite = false)
    }

    protected abstract fun url(): String
}
