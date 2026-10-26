plugins {
    id("scripto.generator")
}

library {
    common {
        api(versions.libs.coordinates("file-io"))
        api(project(":ir"))
        implementation(project(":generator"))
    }
}
