plugins {
    id("scripto.generator")
}

library {
    common {
        api(versions.libs.coordinates("file-io"))
        implementation(versions.libs.coordinates("process"))
        api(project(":ir"))
        implementation(project(":generator"))
    }
}