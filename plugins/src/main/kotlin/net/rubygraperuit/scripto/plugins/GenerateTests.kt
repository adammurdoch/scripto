package net.rubygraperuit.scripto.plugins

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.PrintWriter
import java.nio.file.Path
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
abstract class GenerateTests : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val baseName: Property<String>

    @TaskAction
    fun generate() {
        val outputDir = outputDirectory.get().asFile.toPath()
        outputDir.deleteRecursively()

        val sourceDir = sourceDirectory.get().asFile.toPath()

        var testClasses = 0
        sourceDir.walk().forEach { file ->
            testClasses += generateTestFor(sourceDir.relativize(file).toString(), outputDir)
        }
        if (testClasses == 0) {
            throw RuntimeException("Could not find any test classes in $sourceDir")
        }
    }

    private fun generateTestFor(relativePath: String, outputDir: Path): Int {
        val sourceClassName = relativePath.substringAfterLast("/").removeSuffix(".kt")
        if (sourceClassName == "AbstractIntegrationTest" || sourceClassName == "AppFixture") {
            return 0
        }
        val sourceDirPart = relativePath.substringBeforeLast("/")
        val sourcePackage = sourceDirPart.replace('/', '.')

        val baseName = baseName.get()

        val dirPart = sourceDirPart.removeSuffix("/test") + "/" + baseName.lowercase()
        val packageName = dirPart.replace('/', '.')

        val className = baseName + sourceClassName.removePrefix("Abstract")

        val outputFile = outputDir.resolve("$dirPart/$className.kt")
        outputFile.parent.createDirectories()
        outputFile.bufferedWriter().use { writer ->
            PrintWriter(writer).run {
                println("// generated source")
                println("package $packageName")
                println()
                println("import $sourcePackage.$sourceClassName")
                println()
                println("class ${className}: $sourceClassName() {")
                println("    override fun fixture() = ${baseName}Fixture(workDir)")
                println("}")
            }
        }

        return 1
    }
}