plugins {
    id("net.rubygrapefruit.kmp.lib")
}

library {
    jvm()
    nativeDesktop()
    common {
        api(project(":ir"))
        api(versions.libs.coordinates("file-io"))
    }
}
