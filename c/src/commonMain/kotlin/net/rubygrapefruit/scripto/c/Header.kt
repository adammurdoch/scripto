package net.rubygrapefruit.scripto.c

internal class Header(val name: String) : CDeclaration {
    companion object {
        val Stdint = Header("<stdint.h>")
        val Stdlib = Header("<stdlib.h>")
        val Stdio = Header("<stdio.h>")
        val Stdbool = Header("<stdbool.h>")
        val String = Header("<string.h>")
        val Fcntl = Header("<fcntl.h>")
        val Unistd = Header("<unistd.h>")
        val Stat = Header("<sys/stat.h>")
    }
}