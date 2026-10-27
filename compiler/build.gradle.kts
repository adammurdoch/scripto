plugins {
    id("net.rubygrapefruit.kmp.lib")
    id("de.infix.testBalloon").version("1.1.0")
}

library {
    jvm {
        test {
            implementation("org.jetbrains.kotlin:kotlin-test-junit5:${versions.kotlin.version}")
        }
    }
    nativeDesktop()
    common {
        api(project(":ir"))
        api(versions.libs.coordinates("file-io"))
        implementation(versions.libs.coordinates("basics"))
        implementation(versions.libs.coordinates("parse"))
        implementation(versions.libs.coordinates("file-parse"))
    }
    test {
        implementation("de.infix.testBalloon:testBalloon-framework-core:1.1.0")
        implementation(versions.test.coordinates)
    }
}