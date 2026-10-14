plugins {
    id("net.rubygrapefruit.jvm.lib")
}

library {
    // Uses the JVM's ClassFile API
    targetJvmVersion = 25

    dependencies {
        api(versions.libs.coordinates("file-io"))
        api(project(":ir"))
        implementation(project(":generator"))
    }
    test {
        implementation(project(":test-suite"))
    }
}
