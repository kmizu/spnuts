# SPnuts

A Scala 3 reimplementation of the [Pnuts](https://pnuts.dev.java.net/) scripting language, originally created by Toyokazu Tomatsu (戸松豊和) at Sun Microsystems.

Builds for both **JVM** and **Scala Native** (cross-platform).

## Download SPnuts 0.1.0

Runnable JVM and Linux Native distributions, their checksums, and installation
instructions are published on the [SPnuts release site](https://kmizu.github.io/spnuts/).
GitHub Release assets contain the same files.

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

## Running SPnuts

### Interactive REPL

```bash
sbt "replJVM/run"
```

```
SPnuts 0.1.0 (Scala reimplementation)
Thanks to Tomatsu-san for the original Pnuts.
Type :quit to exit, :help for commands.
pnuts> function fib(n) {
..... if (n <= 1) n
..... else fib(n - 1) + fib(n - 2)
..... }
pnuts> fib(10)
55
pnuts> :quit
```

REPL commands:

| Command | Description |
|---------|-------------|
| `:help` | Show help |
| `:quit` / `:exit` / `:q` | Exit |
| `:load PATH` | Evaluate a script file into the current session |
| `:bindings` | List variables defined so far in this session |

Multi-line input is supported: if a statement is left open (an unclosed
`{`, `(`, or `[`), the prompt changes to `..... ` and keeps reading until
the statement is complete.

### Running a Script File

```bash
sbt "replJVM/run path/to/script.pnuts"
```

Example — `hello.pnuts`:

```pnuts
name = "world"
println("Hello \(name)!")
```

```bash
sbt "replJVM/run hello.pnuts"
# Hello world!
```

### Scala Native

```bash
# Build native binary
sbt "replNative/nativeLink"

# Run the native binary directly
./repl/native/target/scala-3.3.1/spnuts-repl-out

# Or run via sbt
sbt "replNative/run"
```

> **Note:** The Scala Native binary has no JVM startup overhead. Java interop (`new java.util.ArrayList()` etc.) is not available in native mode.

### Build & Test

```bash
# Run all JVM tests
sbt "coreJVM/test"

# Run all Scala Native tests
sbt "coreNative/test"

# Build everything
sbt compile
```

## Mandatory Gradual Typing

SPnuts always type-checks each complete chunk before running it. This is
mandatory behavior: there is no flag to disable or opt into checking.

```pnuts
var count = 1        // inferred as Long
count = count + 1    // OK
count = "two"        // Type error before this chunk runs

val ratio: Double = 1  // Long -> Double widening

function twice(x: Long): Long x * 2

function inspect(value) { // value is Any
  type(value)
}
```

Unannotated bindings still receive an inferred, fixed type. `val` is
immutable; `var` and the legacy assignment form (`name = value`) are mutable,
but later values must remain compatible with the binding's type. The only
implicit numeric widening is `Long` to `Double`.

`Any` marks dynamic boundaries, including unannotated parameters and values
coming from host, Java, or `eval` integration. Compatibility is structural and
recursive, so `Any` also works inside collection and function types; existing
runtime checks still protect concrete annotations when an `Any` value crosses
such a boundary. An empty source list has type `List<Any>`, while an empty map
expression constructed through the AST API has type `Map<Any, Any>`.
Incompatible branch result types join to `Any`.

Successful inferred types persist across REPL or evaluation chunks and are
tracked separately by package. A chunk with a type error neither runs nor
publishes its inferred type state.

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
for (x : range_gen(5)) print(str(x) + " ")  // 0 1 2 3 4
println(join(toList(range_gen(3)), ", "))     // 0, 1, 2

// Map literals
m = { "a" => 1, "b" => 2 }

// Try/catch
try {
  throw "oops"
} catch (e: java.lang.RuntimeException) {
  println("caught: " + e.getMessage())
} catch (e) {
  println("catch-all: " + e.getMessage())
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
- Scala 3.3.1 (LTS)
- JDK 17+
- For Scala Native builds: LLVM/Clang toolchain + Scala Native 0.5.6

## Background

Pnuts was a JVM scripting language created by Toyokazu Tomatsu (戸松豊和) during his time at Sun Microsystems. After Sun's acquisition by Oracle, the language went unmaintained for many years.

Some time ago, [kmizu](https://github.com/kmizu) reached out to Tomatsu-san and was graciously given permission to take over the project. Despite that, there was no opportunity to actually implement a new version — until now.

The trigger for this reimplementation was [Claude Code](https://claude.ai/code). Using it as a pair-programming partner made it finally feasible to rewrite Pnuts from scratch in Scala 3, with a hand-written PEG parser replacing the original JavaCC grammar, and cross-platform support via Scala Native.

## License

Same as the original Pnuts — see [LICENSE](LICENSE).
