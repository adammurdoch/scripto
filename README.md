
A strongly typed scripting language that compiles to C, JVM byte code, Bash and PowerShell.

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
- Function
- Static Function

Standard library
----

- `println()`
- `println(String)`
- `println(Int)`
- `println(Boolean)`
- `println(File)`

String type
- `"` quoted literal
- `String.toString()`

Int type
- Int literal
- `Int.toString()`

Boolean type
- `true` or `false` literal
- `Boolean.toString()`

File type
- `File(String)` constructor
- `File.toString()`
- `File.readText(): String`

List Type
- `[...]` literal
- `List(...)` constructor
- `List.toString()`

Operators
- `Int + Int: Boolean`
- `String + String: String`
- `String == String: Boolean`
- `Int == Int: Boolean`
- `Boolean == Boolean: Boolean`
