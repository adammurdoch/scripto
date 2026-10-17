plugins {
    id("net.rubygrapefruit.gradle-plugin")
}

pluginBundle {
    plugin("scripto.powershell", "net.rubygraperuit.scripto.plugins.PowerShellPlugin")

    dependencies {
        implementation(versions.libs.coordinates("download"))
    }
}