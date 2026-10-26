plugins {
    id("scripto.generator")
}

library {
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
}
