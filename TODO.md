
General

- Improve error message when operator called with arguments of incorrect type
- Inline constants
  - Evaluate functions with constant args at compile time
  - Need some way to initialize variables to arbitrary value in order to test this, e.g. function defs or args to main
- Eliminate unused variables
- Escapes in strings
- Define int size
- Consistent formatting for parse errors and semantic errors 

Stdlib

- Split println() into type specific overloads
- readText() fails when file cannot be read

Interpreter

- Encode `File` as `RegularFile`

JVM

- Encode `File` as JVM `Path`
- Include debug information

C

- Don't use a temporary variable to calculate variable initializer, e.g. for `Int.toString()`
- Garbage collect strings created by `+` operator, `Int.toString()` and `File.readText()`
- Add a helper function for `Int.toString()`, similar to `stringConcat()` 
- Add a helper function to `File.toString()`
- `Int.toString()`: Don't use a temporary variable when value is already a variable reference
  - Need some other `Int` typed expression to use for current test coverage, eg a function that returns `Int` or a property of type `Int`
- Use `whar_t*` or `char16_t*` to represent strings?
- Interesting reference counting approach https://www.microsoft.com/en-us/research/wp-content/uploads/2020/11/perceus-tr-v1.pdf

Bash

- Don't use a temporary variable to calculate variable initializer, e.g. for `File.readText()`
- Don't wrap '+' operators in paren when already in arithmetic mode
- Handle case where multiple expressions use a temporary variable
- Use `declare -i` for int variables?

PowerShell

- Don't use a temporary variable to calculate variable initializer, e.g. for `File.readText()`
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
- function type + literal
- named function declarations
- structs
