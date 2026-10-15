
A strongly typed scripting language that compiles to C, JVM byte code, Bash and PowerShell.

Language
----

program = statement*

comment = `//` (not end-of-line)* end-of-line

statement = function call | variable declaration

function call = identifier `(` expression (`,` expression)* `)`

expression = literal | variable reference | infix | function call

literal = string literal | int literal | boolean literal

string literal = `"` text `"`

int literal = `0` | `-`? `1`-`9` (`0`-`9`)*

boolean literal = `true` | `false`

infix = equality | addition

equality = expression `==` expression

addition = expression `+` expression

variable reference = identifier

variable declaration = `val` identifier `=` expression

identifier = (`a`-`z` | `A`-`Z`) (`a`-`z` | `A`-`Z` | `0`-`9`)*

Standard library
----

- `println()`
- `println(String)`
- `println(Int)`
- `println(Boolean)`
- `File(String)`
- `readText(String): String`
- `Int + Int: Boolean`
- `String + String: String`
- `String == String: Boolean`
- `Int == Int: Boolean`
- `Boolean == Boolean: Boolean`
