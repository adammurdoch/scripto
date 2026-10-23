General

- Lists
  - Infer element type  
  - `toString()` delegates to correct implementation
  - Add `==`
  - Add `[]`
  - Add `+`
  - Inline list literal `List.toString()` and `println(List)`
- Improve error message when operator called with arguments of incorrect type
- Inline constants
    - Evaluate functions with constant args at compile time
    - Need some way to initialize variables to arbitrary value in order to test this, e.g. function defs or args to main
- Eliminate unused variables
- Escapes in strings
- Define int size
- Consistent formatting for parse errors and semantic errors
- Trailing commas in list literal, parameter lists and argument lists

Stdlib

- Split `println()` into type specific overloads
- `readText()` fails when file cannot be read

Interpreter

- Encode `File` as `RegularFile`
    - Need to get relative path for `toString()` implementation

JVM

- Use `StringConcatFactory` for string concatenation
- Include debug information

C

- Use an allocator for `List.toString()`
- Add a string builder and use for `List.toString()`, and `String + String`
- Garbage collect strings created by `+` operator, `Int.toString()`, `File.toString()`, `List.toString()` and `File.readText()`
- Add a helper function for `File.toString()`?
- Use `whar_t*` or `char16_t*` to represent strings?
- Interesting reference counting approach https://www.microsoft.com/en-us/research/wp-content/uploads/2020/11/perceus-tr-v1.pdf

Bash

- Implement `List.toString()` when receiver is not a variable reference
- Strongly type `BashExpression` types that can be used in bash commands and those that can be used in arithmetic contexts?
- Don't use a temporary variable to calculate variable initializer, e.g. for `File.readText()`
- Reuse temporary variables across statements
- Add a helper function for `List.toString()`?
- Don't wrap '+' operators in paren when already in arithmetic mode
- Use `declare -i` for int variables?

PowerShell

- Strongly type array variables 
- Don't use a temporary variable to calculate variable initializer, e.g. for `File.readText()`
- Reuse temporary variables across statements
- Use string interpolation for `File.toString()`?
- Don't wrap '+' operators in `$()` or `()` when already in arithmetic mode

Webassembly

- Component model: https://component-model.bytecodealliance.org
- Runtime: https://github.com/bytecodealliance/wasmtime

Performance

- Don't read source file content as a single String

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
- `readText()` becomes `File(string)` -> `File` and `File.readText()` -> `result<string>`
- `file.exists()` -> `boolean`
- `file.writeText(string, string)` -> `result<unit>`
- `homeDir()` -> `dir`
- `dir.exists()` -> `boolean`

More features

- `if` statement
- string interpolation
- function type + literal
- named function declarations
- structs
- release vs debug executable (e.g. no range checks, etc.)
