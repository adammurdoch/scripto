package net.rubygrapefruit.scripto.c

internal sealed class ExternalDeclaration {
    open fun visitHeaders(visitor: (Header) -> Unit) {
    }
}

internal class Header(val name: String) : ExternalDeclaration() {
    companion object {
        val Stdlib = Header("<stdlib.h>")
        val Stdio = Header("<stdio.h>")
        val Stdbool = Header("<stdbool.h>")
        val String = Header("<string.h>")
        val Fcntl = Header("<fcntl.h>")
        val Unistd = Header("<unistd.h>")
        val Stat = Header("<sys/stat.h>")
    }
}

internal class SourceFragment(val headers: List<Header>, text: String) : ExternalDeclaration() {
    val text: String = text.trim() + "\n"

    override fun visitHeaders(visitor: (Header) -> Unit) {
        headers.forEach { visitor(it) }
    }

    companion object {
        val ReadText = SourceFragment(
            listOf(Header.Stdlib, Header.Fcntl, Header.Unistd, Header.Stat),
            """
char* readText(const char* fileName) {
    int fd = open(fileName, O_RDONLY);
    if (fd < 0) {
        return "ERROR";
    }
    struct stat fileInfo;
    if (fstat(fd, &fileInfo) != 0) {
        close(fd);
        return "ERROR";
    }
    if (fileInfo.st_size == 0) {
        close(fd);
        return "";
    }
    char *buf = malloc(fileInfo.st_size+1);
    if (buf == NULL) {
        close(fd);
        return "ERROR";
    }
    if (read(fd, buf, fileInfo.st_size) < 0) {
        close(fd);
        free(buf);
        return "ERROR";
    }
    buf[fileInfo.st_size] = 0;
    close(fd);
    return buf;
}
        """
        )

        val StringConcatenation = SourceFragment(
            listOf(Header.String, Header.Stdlib), """
const char* stringConcat(const char* left, const char* right) {
    size_t leftLen = strlen(left);
    if (leftLen == 0) {
        return right;
    }
    size_t len = leftLen + strlen(right);
    char* buffer = malloc(len + 1);
    strcpy(buffer, left);
    strcpy(buffer + leftLen, right);
    return buffer;
}
        """.trimIndent()
        )
    }
}
