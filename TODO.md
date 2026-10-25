General

- Lists
  - Fail when list elements have different type (for now) 
    - Handle a list of lists where some elements are empty, eg `[[1, 2], [], [3]]`
  - `toString()` delegates to correct implementation
  - Add `args` parameter to main function 
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
- Improve experience when unsupported feature is used
  - For example, script can declare targets and so compilation fails when feature is used
  - For example, list targets when invoking compiler and fail when feature is used

Stdlib

- `readText()` fails when file cannot be read

Interpreter

- Encode `File` as `RegularFile`
    - Need to get original path for `toString()` implementation

JVM

- Use `StringConcatFactory` for string concatenation
- Set locals that are not referenced later in the function to null
- Reuse local slots
- Include debug information

C

- Bundle or download C compiler and libc to build executable, rather than relying on installed compiler
- Garbage collect strings created by `+` operator, `Int.toString()`, `File.toString()`, `List.toString()` and `File.readText()`
  - Variable values
  - Function arguments
- Use an allocator for `List.toString()`
  - macOS/Linux: use mmap() + madvise() to free up chunks, can also use malloc()
  - Windows: VirtualAlloc() (reserve buffer, then commit chunks as required) + VirtualFree() to uncommit chunks
- Add a string builder and use for `List.toString()`, and `String + String`
- Reuse temporary variables across statements
- Add a helper function for `File.toString()`?
- Use `whar_t*` or `char16_t*` to represent strings?
- Lists
  - Pack metadata and elements into a single byte array 
  - Don't store list size if it is known at compile time
  - Declaring a max size allows inlining into something else
  - Encode List<Boolean> as a bit field
  - Encode length as a variable length int? https://en.wikipedia.org/wiki/LEB128
- Memory safety
  - Construct data structures directly into their final location e.g. `val l = [a] + someList()` can pass `someList()` a pointer to where to place its result
  - References within a structure can be relative offsets, so the structure can be easily moved or copied
  - Function metadata indicates which params the result references
  - Function metadata indicates whether object layout changes
  - For example, can combine the above to replace with a copy or perhaps fail
- Interesting reference counting approach https://www.microsoft.com/en-us/research/wp-content/uploads/2020/11/perceus-tr-v1.pdf

Bash

- Maybe support nested lists?
  - e.g. by encoding list as `list.flatMap { nested -> listOf(nested.size) + nested }`
- Report "List<List<*>> is not supported" warning the same way as semantic errors, and include source location information
  - Do this even if compilation fails
- Strongly type `BashExpression` types that can be used in bash commands and those that can be used in arithmetic contexts?
- When a variable's value is a temporary variable, use the temporary variable's value directly
  - e.g. `val x = a == b` could generate an if statement that sets the value of `x` instead of a temporary variable 
- Reuse temporary variables across statements
- Unset variables that are no longer referenced
- Add a helper function for `List.toString()`?
- Don't wrap '+' operators in paren when already in arithmetic mode
- Use `declare` for int and array variables?

PowerShell

- Declare variable types 
- Cannot use `Write-Output` inside a function, as this adds the output to the function return value
- Use string interpolation for `File.toString()`?
- Don't wrap '+' operators in `$()` or `()` when already in arithmetic mode
- PowerShell scripts can declare named and positional command line parameters and their types, so provide some way to declare this 

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
- maps
- release vs debug executable (e.g. no range checks, overflow checks, etc.)
