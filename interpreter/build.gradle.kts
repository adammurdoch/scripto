plugins {
    id("scripto.generator")
}

library {
    common {
        implementation(versions.libs.coordinates("file-io"))
        implementation(project(":ir"))
    }
}
