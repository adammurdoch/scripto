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

internal sealed class Snippet(val declarations: List<CDeclaration>, text: String) : CDeclaration {
    val text: String = text.trim() + "\n"

    override fun visitOwnDeclarations(visitor: (CDeclaration) -> Unit) {
        declarations.forEach(visitor)
    }
}

internal class TypeSnippet(name: String, declarations: List<CDeclaration>, text: String) : Snippet(declarations, text), CType {
    override val declaration: String = "struct $name"

    override fun visitDeclarations(visitor: (CDeclaration) -> Unit) {
        visitor(this)
    }

    companion object {
        val AnyStruct = TypeSnippet(
            "_any",
            emptyList(),
            """
struct _any {
};
            """.trimIndent()
        )
        val ListStruct = TypeSnippet(
            "_list",
            listOf(Header.Stdint),
            """
struct _list {
    int32_t length;
    void* elements;
};
            """.trimIndent()
        )
    }
}

internal class FunctionSnippet(override val name: String, declarations: List<CDeclaration>, text: String) : Snippet(declarations, text), CFunctionDeclaration {
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
            listOf(Header.Stdlib, Header.Stdio), """
const char* _int_to_string(int value) {
    size_t len = snprintf(NULL, 0, "%d", value) + 1;
    char* buffer = malloc(len);
    snprintf(buffer, len, "%d", value);
    return buffer;
}
            """.trimIndent()
        )

        val StringListToString = FunctionSnippet(
            "_string_list_to_string",
            listOf(TypeSnippet.ListStruct, Header.String, Header.Stdlib), """
const char* _string_list_to_string(struct _list* list) {
    size_t str_len = 3;
    for (int i = 0; i < list->length; i++) {
        str_len += strlen(((char**)list->elements)[i]);
    }
    if (list->length > 1) {
        str_len += (list->length-1) * 2;
    }
    char* result = malloc(sizeof(char)*str_len);
    char* current = result;
    *current = '[';
    current++;
    for (int i = 0; i < list->length; i++) {
        if (i > 0) {
            *current = ',';
            current++;
            *current = ' ';
            current++;
        }
        current = stpcpy(current, ((char**)list->elements)[i]);
    }
    *current = ']';
    current++;
    *current = 0;
    return result;
}
            """.trimIndent()
        )

        fun listElementToStringFunction(name: String, elementType: CType, toString: (CExpression) -> CExpression): CGeneratedFunction {
            val getElement = CArrayIndex(
                CCast(
                    elementType.pointer(),
                    CStructField(
                        CVariableReference("list"),
                        "elements"
                    )
                ),
                CVariableReference("index")
            )
            val elementToString = toString(getElement)
            return CGeneratedFunction(
                name,
                CType.CString,
                listOf(
                    Parameter("index", CType.CInt),
                    Parameter("list", TypeSnippet.ListStruct.pointer())
                ),
                listOf(
                    CReturnStatement(elementToString)
                )
            )
        }

        val ListToString = FunctionSnippet(
            "_list_to_string",
            listOf(TypeSnippet.ListStruct, Header.String, Header.Stdlib, Header.Stdbool, StringListToString), """
const char* _list_to_string(struct _list* list, bool free_strings, const char* (*map)(int, struct _list*)) {
    const char* mapped[list->length];
    for (int i = 0; i < list->length; i++) {
        mapped[i] = map(i, list);
    }
    struct _list str_list = {list->length, mapped};
    const char* result = _string_list_to_string(&str_list);
    if (free_strings) {
        for (int i = 0; i < list->length; i++) {
            free((void*)mapped[i]);
        }
    }
    return result;
}
            """.trimIndent()
        )
    }
}
