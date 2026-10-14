
Language
----

program = statement*

comment = `//` (not end-of-line)* end-of-line

statement = function call | variable declaration

function call = name `(` expression (`,` expression)* `)`

expression = literal | variable reference | infix | function call

literal = string literal | int literal | boolean literal

string literal = `"` text `"`

int literal = `0` | `-`? `1`-`9` (`0`-`9`)*

boolean literal = `true` | `false`

infix = equality | addition

equality = expression `==` expression

addition = expression `+` expression

variable reference = name

variable declaration = `val` name `=` expression

name = `a`-`z` (`a`-`z` | `0`-`9`)*

Standard library
----

- `println()`
- `println(String)`
- `println(Int)`
- `println(Boolean)`
- `readText(String): String`
- `Int + Int: Boolean`
- `String + String: String`
- `String == String: Boolean`
- `Int == Int: Boolean`
- `Boolean == Boolean: Boolean`
