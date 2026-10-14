plugins {
    id("net.rubygrapefruit.kmp.lib")
}

library {
    jvm()
    macOS()
    common {
        api(project(":ir"))
        api(versions.libs.coordinates("file-io"))
    }
}
