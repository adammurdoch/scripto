plugins {
    id("scripto.generator")
    id("scripto.node-js")
}

library {
    jvm {
        test {
            implementation(versions.libs.coordinates("machine-info"))
        }
    }
    common {
        api(versions.libs.coordinates("file-io"))
        api(project(":ir"))
        implementation(project(":generator"))
    }
}
