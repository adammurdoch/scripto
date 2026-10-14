plugins {
    id("net.rubygrapefruit.kmp.lib")
}

library {
    jvm()
    macOS()

    common {
        api(versions.libs.coordinates("file-io"))
        api(project(":ir"))
        implementation(project(":generator"))
    }
    test {
        implementation(project(":test-suite"))
    }
}
