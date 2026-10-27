plugins {
    id("net.rubygrapefruit.gradle-plugin")
}

pluginBundle {
    plugin("scripto.library", "net.rubygraperuit.scripto.plugins.LibraryPlugin")
    plugin("scripto.generator", "net.rubygraperuit.scripto.plugins.GeneratorPlugin")
    plugin("scripto.generator-test-suite", "net.rubygraperuit.scripto.plugins.GeneratorTestSuitePlugin")
    plugin("scripto.powershell", "net.rubygraperuit.scripto.plugins.PowerShellPlugin")
    plugin("scripto.node-js", "net.rubygraperuit.scripto.plugins.NodeJsPlugin")

    dependencies {
        implementation(versions.libs.coordinates("machine-info"))
        implementation(versions.libs.coordinates("download"))
        implementation(versions.plugins.basePluginsCoordinates)
        implementation("de.infix.testBalloon:de.infix.testBalloon.gradle.plugin:1.2.0")
    }
}