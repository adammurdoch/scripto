plugins {
    id("net.rubygrapefruit.native.cli-app")
}

application {
    appName = "scripto"

    nativeDesktop()

    common {
        implementation(versions.libs.coordinates("file-io"))
        implementation(versions.libs.coordinates("cli-app"))
        implementation(project(":compiler"))
        implementation(project(":bash"))
        implementation(project(":c"))
        implementation(project(":jvm"))
        implementation(project(":node"))
        implementation(project(":powershell"))
        implementation(project(":interpreter"))
    }
}