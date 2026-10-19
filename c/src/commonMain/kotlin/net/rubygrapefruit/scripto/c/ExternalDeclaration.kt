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

internal class FunctionSnippet(val name: String, val headers: List<Header>, text: String) : ExternalDeclaration() {
    val text: String = text.trim() + "\n"

    override fun visitHeaders(visitor: (Header) -> Unit) {
        headers.forEach { visitor(it) }
    }

    companion object {
        val ReadText = FunctionSnippet(
            "_read_text",
            listOf(Header.Stdlib, Header.Fcntl, Header.Unistd, Header.Stat),
            """
char* _read_text(const char* fileName) {
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

        val StringConcatenation = FunctionSnippet(
            "_string_concat",
            listOf(Header.String, Header.Stdlib), """
const char* _string_concat(const char* left, const char* right) {
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

        val IntToString = FunctionSnippet(
            "_int_to_string",
            listOf(Header.Stdlib), """
const char* _int_to_string(int value) {
    size_t len = snprintf(NULL, 0, "%d", value) + 1;
    char* buffer = malloc(len);
    snprintf(buffer, len, "%d", value);
    return buffer;
}
            """.trimIndent()
        )
    }
}
