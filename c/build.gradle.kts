plugins {
    id("net.rubygrapefruit.kmp.lib")
    id("scripto.test-suite")
}

library {
    jvm()
    macOS {
        dependencies {
            implementation(versions.libs.coordinates("process"))
        }
    }

    common {
        api(versions.libs.coordinates("file-io"))
        api(project(":ir"))
        implementation(project(":generator"))
    }
    test {
        implementation(project(":test-suite"))
    }
}
