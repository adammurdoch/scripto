
General
- Improve error message when operator called with arguments of incorrect type
- Inline constants
  - Evaluate functions with constant args at compile time
- Eliminate unused variables
- Escapes in strings
- Define int size

Stdlib
- Split println() into type specific overloads
- readText() fails when file cannot be read

JVM
- Include debug information

C
- Garbage collect strings
- Use puts() instead of printf()
- Use `whar_t*` or `char16_t*` to represent strings?
- Interesting reference counting approach https://www.microsoft.com/en-us/research/wp-content/uploads/2020/11/perceus-tr-v1.pdf

Bash
- Handle case where multiple expressions use a temporary variable
- Use `declare -i` for int variables?

Webassembly

- Component model: https://component-model.bytecodealliance.org
- Runtime: https://github.com/bytecodealliance/wasmtime

New functions and types

- `boolean && boolean` -> `boolean`
- `boolean || boolean` -> `boolean`
- `!boolean` -> `boolean`
- `string.length()`
- `string.contains(string)`
- `string.replace(string, string)`
- `boolean.toString()`
- `int.toString()`
- `[element*]` -> `list<T>`
- `readText()` becomes `file(string)` -> `file` and `file.readText()` -> `result<string>`
- `file.exists()` -> `boolean`
- `file.writeText(string, string)` -> `result<unit>`
- `homeDir()` -> `dir`
- `dir.exists()` -> `boolean`

More features

- `if` statement
- function type + literal
- named function declarations
- structs
