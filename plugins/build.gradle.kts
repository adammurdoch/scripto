plugins {
    id("net.rubygrapefruit.gradle-plugin")
}

pluginBundle {
    plugin("scripto.powershell", "net.rubygraperuit.scripto.plugins.PowerShellPlugin")
    plugin("scripto.test-suite", "net.rubygraperuit.scripto.plugins.TestSuitePlugin")

    dependencies {
        implementation(versions.libs.coordinates("download"))
        implementation(versions.plugins.basePluginsCoordinates)
    }
}