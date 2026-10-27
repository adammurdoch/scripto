plugins {
    id("scripto.library")
}

library {
    common {
        api(project(":ir"))
        api(versions.libs.coordinates("file-io"))
    }
}
