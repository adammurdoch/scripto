plugins {
    id("net.rubygrapefruit.jvm.cli-app")
}

application {
    targetJvmVersion = 25
    appName = "scripto"

    dependencies {
        implementation(versions.libs.coordinates("file-io"))
        implementation(versions.libs.coordinates("cli-app"))
        implementation(project(":compiler"))
        implementation(project(":bash"))
        implementation(project(":powershell"))
        implementation(project(":c"))
        implementation(project(":jvm"))
        implementation(project(":interpreter"))
    }
}