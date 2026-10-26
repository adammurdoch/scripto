plugins {
    id("scripto.generator")
    id("scripto.powershell")
}

library {
    common {
        api(versions.libs.coordinates("file-io"))
        api(project(":ir"))
        implementation(project(":generator"))
    }
}
