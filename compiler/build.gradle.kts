plugins {
    id("net.rubygrapefruit.kmp.lib")
}

library {
    jvm()
    macOS()
    common {
        api(project(":ir"))
        api(versions.libs.coordinates("file-io"))
        implementation(versions.libs.coordinates("parse"))
        implementation(versions.libs.coordinates("file-parse"))
    }
    test {
        implementation(versions.test.coordinates)
    }
}