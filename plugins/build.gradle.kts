plugins {
    id("net.rubygrapefruit.gradle-plugin")
}

pluginBundle {
    plugin("scripto.generator", "net.rubygraperuit.scripto.plugins.GeneratorPlugin")
    plugin("scripto.test-suite", "net.rubygraperuit.scripto.plugins.TestSuitePlugin")
    plugin("scripto.powershell", "net.rubygraperuit.scripto.plugins.PowerShellPlugin")
    plugin("scripto.node-js", "net.rubygraperuit.scripto.plugins.NodeJsPlugin")

    dependencies {
        implementation(versions.libs.coordinates("machine-info"))
        implementation(versions.libs.coordinates("download"))
        implementation(versions.plugins.basePluginsCoordinates)
    }
}