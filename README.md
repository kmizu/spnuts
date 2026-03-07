# SPnuts

A Scala 3 reimplementation of the [Pnuts](https://pnuts.dev.java.net/) scripting language, originally created by Toyomasa Watarai (戸松豊雅) at Sun Microsystems.

Builds for both **JVM** and **Scala Native** (cross-platform).

## Features

- Hand-written PEG lexer/parser — no parser generator needed
- Tree-walking interpreter
- Closures and lexical scoping
- Generators (`yield`)
- String interpolation: `"Hello \(name)!"`
- For-each with range: `for (x : 1..10)`
- Java interop via reflection (JVM)
- 80+ built-in functions
- Cross-build: JVM + Scala Native

## Quick Start

```bash
# Run the REPL
sbt "replJVM/run"

# Run tests
sbt "coreJVM/test"

# Compile for Scala Native
sbt "coreNative/compile"
```

## Language Overview

```pnuts
// Variables
x = 42
name = "world"

// String interpolation
println("Hello \(name)!")

// Functions
function fib(n)
  if (n <= 1) n
  else fib(n - 1) + fib(n - 2)

println(fib(10))  // 55

// Closures
double = { x -> x * 2 }
println(double(21))  // 42

// For-each with range
sum = 0
for (x : 1..100) sum = sum + x
println(sum)  // 5050

// Higher-order functions
result = map([1, 2, 3, 4, 5], { n -> n * n })
println(join(result, ", "))  // 1, 4, 9, 16, 25

// Generators
function range_gen(n) {
  i = 0
  while (i < n) { yield i; i = i + 1 }
}
println(range_gen(5))  // [0, 1, 2, 3, 4]

// Map literals
m = { "a" => 1, "b" => 2 }

// Try/catch
try {
  throw "oops"
} catch (java.lang.RuntimeException e) {
  println("caught: \(e.getMessage())")
}

// Switch
switch (x) {
  case 1: println("one"); break
  case 2: println("two"); break
  default: println("other")
}
```

## Built-in Functions

| Category | Functions |
|---|---|
| I/O | `print`, `println`, `p` |
| Type conversion | `str`, `int`, `float`, `boolean`, `char` |
| Type info | `type`, `isNull`, `isString`, `isArray`, `isNumber` |
| Collections | `size`, `length`, `isEmpty`, `array`, `list`, `map` |
| Higher-order | `map`, `filter`, `reduce`, `each`, `any`, `all` |
| List ops | `sort`, `reverse`, `append`, `get`, `put`, `first`, `last`, `remove`, `copy`, `flatten` |
| Map ops | `keys`, `values`, `contains` |
| Range | `range` |
| String | `join`, `split`, `trim`, `toUpperCase`, `toLowerCase`, `startsWith`, `endsWith`, `indexOf`, `substring`, `replace`, `format`, `concat`, `charAt`, `matches`, `replaceAll` |
| Math | `abs`, `max`, `min`, `pow`, `sqrt`, `floor`, `ceil`, `round`, `log`, `sin`, `cos`, `tan`, `PI`, `E`, `random` |
| Misc | `eval`, `generator`, `assert`, `error`, `sleep` |

## Project Structure

```
spnuts/
  build.sbt
  core/
    shared/src/main/scala/spnuts/
      ast/          # Sealed trait AST hierarchy
      parser/       # Hand-written lexer + PEG parser
      interpreter/  # Tree-walking interpreter
      runtime/      # Context, functions, operators, builtins
    jvm/            # Java interop (reflection)
    native/         # Scala Native stubs
  repl/
    shared/         # REPL base class
    jvm/            # JLine3 REPL
    native/         # Native REPL
```

## Requirements

- sbt 1.9+
- Scala 3.3.1
- JDK 11+
- (Optional) Scala Native 0.5.6 toolchain for native builds

## Background

Pnuts was a Java-embedded scripting language developed at Sun Microsystems by Toyomasa Watarai (戸松豊雅). This reimplementation is done with permission, using a hand-written PEG parser based on the original `Pnuts.jjt` grammar.

## License

Same as the original Pnuts — see [LICENSE](LICENSE).
