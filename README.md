
A statically typed language with automatic memory management that compiles to:

- Native executables (macOS)
- C source
- JVM byte code
- Node.js scripts
- Bash scripts
- PowerShell scripts

The compiler runs on macOS. It is also able to run a source file without compiling it to an output file
 
Motivation
----

This is an experiment to see how feasible it is to implement a simple, but useful, language that can run in many different environments.

Status
----

Pre-experimental

Language
----

program = statement*

comment = `//` (not end-of-line)* end-of-line

statement = function call | variable declaration

function call = (expression `.` )? identifier `(` expression (`,` expression)* `)`

expression = literal | variable reference | infix | function call

literal = string literal | int literal | boolean literal | list literal

string literal = `"` text `"`

int literal = `0` | `-`? `1`-`9` (`0`-`9`)*

boolean literal = `true` | `false`

list literal = `[` expression* `]`

infix = equality | addition

equality = expression `==` expression

addition = expression `+` expression

variable reference = identifier

variable declaration = `val` identifier `=` expression

identifier = (`a`-`z` | `A`-`Z`) (`a`-`z` | `A`-`Z` | `0`-`9`)*

Terminology
----

- Type
- Function - top level
- Receiver Function

Standard library
----

- `args: List<String>` - the command-line arguments
- `println()`
- `println(Any)`

`String` type - an immutable sequence of characters
- `"` quoted literal
- `String.toString()`

`Int` type - a 32-bit signed int
- Int literal
- `Int.toString()`

`Boolean` type
- `true` or `false` literal
- `Boolean.toString()`

`File` type - a reference to a regular file
- `File(String)` constructor
- `File.toString()`
- `File.readText(): String` - reads the contents of the file as UTF-8 encoded text

`List<T>` Type - an immutable list with elements of type `T`
- `[T...]` literal
- `List(T...)` constructor
- `List.toString()`

Operators
- `Int + Int: Boolean`
- `String + String: String` - concatenates the two strings
- `String == String: Boolean`
- `Int == Int: Boolean`
- `Boolean == Boolean: Boolean`
