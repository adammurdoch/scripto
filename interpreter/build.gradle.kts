plugins {
    id("net.rubygrapefruit.kmp.lib")
    id("scripto.test-suite")
}

library {
    jvm()
    macOS()

    common {
        implementation(versions.libs.coordinates("file-io"))
        implementation(project(":ir"))
    }
    test {
        implementation(project(":test-suite"))
    }
}
