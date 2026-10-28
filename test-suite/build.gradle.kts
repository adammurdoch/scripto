plugins {
    id("net.rubygrapefruit.kmp.lib")
}

library {
    jvm {
        dependencies {
            api(versions.test.junit.coordinates)
            implementation(versions.libs.coordinates("machine-info"))
        }
    }
    nativeDesktop()

    common {
        api(versions.libs.coordinates("file-io"))
        implementation(project(":compiler"))
        implementation(versions.test.coordinates)
    }
}
