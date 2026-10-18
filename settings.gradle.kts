pluginManagement {
    includeBuild("kotlin-tools")
    includeBuild("plugins")
}

plugins {
    id("net.rubygrapefruit.kotlin-base")
}

include("ir")
include("compiler")
include("generator")
include("bash")
include("c")
include("jvm")
include("powershell")
include("interpreter")
include("cli")
include("test-suite")
