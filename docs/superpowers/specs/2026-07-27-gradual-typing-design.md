# Mandatory Gradual Typing for SPnuts

Date: 2026-07-27
Status: Approved by Kouta-senpai

## Goal

Give SPnuts a practical gradual type system while preserving the exploratory,
dynamic feel of Pnuts.

Type checking is part of normal execution. There is no `--check` mode and no
opt-in checker: every parsed program receives static types before any of its
expressions run. Adding annotations tightens those inferred types
automatically.

The defining examples are:

```pnuts
x = 1
x = "later"   // type error before the chunk starts executing
```

and:

```pnuts
val answer: Long = 42
var ratio = 1.0
```

The first binding gives an unannotated variable a type. An explicit annotation
constrains the initializer and later uses. When useful information cannot be
known statically, the type is `Any`, not "untyped".

## Existing Behavior and the Gap

SPnuts already has:

- parser support for annotations on `val`, `var`, function parameters, and
  return types;
- runtime checks for annotated declarations, calls, and returns;
- runtime-class inference for local `val`/`var` bindings;
- numeric coercion at typed call boundaries; and
- JVM/Native shared interpreter and REPL paths.

Those checks happen while the program is running, after earlier expressions may
already have caused side effects. They also use runtime `Class` values as the
type model, which cannot express collection elements or functions portably and
does not exist as a sound shared abstraction for Scala Native.

This design adds a distinct, platform-neutral typing phase before evaluation.
Existing runtime checks remain as defense at dynamic boundaries.

## Principles

1. **Everything has a type.** Every expression and binding is assigned a
   `StaticType`; missing information produces `Any`.
2. **Checking is mandatory.** The outermost interpreter entry point checks the
   complete AST before evaluating its first expression.
3. **Annotations constrain inference.** They do not activate a separate mode.
4. **Reject only proven contradictions.** Operations involving `Any` remain
   legal and are checked at runtime where concrete values become available.
5. **Preserve Pnuts compatibility.** Unknown package bindings, Java interop,
   reflection, and string `eval` are gradual boundaries rather than reasons to
   reject otherwise valid dynamic programs.
6. **Keep static and runtime models separate.** Static typing never depends on
   JVM `Class`; reflection stays in runtime/JVM code.

## Static Type Model

Add `spnuts.typing.StaticType` in `core/shared`:

```text
Any
Null
Unit
Boolean
Long
Double
Char
String
List[element]
Map[key, value]
Array[element]
Function[parameters, return, varargs]
Named[qualified name, type arguments]
TypeVariable[name]
```

`Any` is the gradual top/dynamic type. It is not the absence of a type.

`Named` preserves source-level class and record names without loading classes.
Short aliases normalize into the semantic primitives where possible:

- `Int`, `Short`, and `Byte` normalize to `Long`, matching the interpreter's
  integer literal and arithmetic representation;
- `Float` normalizes to `Double`;
- `Long`, `Double`, `Boolean`, `Char`, `String`, and `Unit` map directly;
- `?` maps to `Any`;
- `Any` maps to `Any`.

Parameterized `List`, `Map`, and arrays retain their element types. Other
parameterized names remain `Named`.

## Compatibility and Joining

Assignment/call/return compatibility is:

- identical types are compatible;
- `Long` is compatible with expected `Double` (the only implicit numeric
  widening);
- `Null` is compatible with `Any`, collections, functions, arrays, and named
  reference types, but not primitive value types;
- a value of static type `Any` is permitted where a concrete type is expected,
  with the existing runtime boundary check responsible for the actual value;
- every concrete value is compatible with expected `Any`;
- collection and function components are checked structurally when both sides
  are known, and otherwise degrade conservatively through `Any`.

Branch and literal inference uses a `join` operation:

- identical types join to themselves;
- `Long` and `Double` join to `Double`;
- `Null` and a reference type join to that reference type;
- collection types join their components recursively;
- incompatible known types join to `Any`.

No union type is introduced in this first slice. `Any` is the deliberate
fallback for incompatible branches.

## Inference Rules

### Bindings

- `val x = expr`: infer the type of `expr`; the binding is immutable.
- `var x = expr`: infer the type of `expr`; later assignments must be
  compatible with that fixed type.
- legacy first assignment `x = expr`: if `x` is not already visible, create a
  mutable binding with the inferred type. Later assignments must be compatible.
- an explicit annotation is the binding type and the initializer must be
  compatible with it.
- `null` without an expected type infers `Null`; assigning a non-null value
  later is allowed only when a declared reference type supplies the intended
  type. A legacy `x = null` therefore remains a `Null` binding rather than
  silently changing type on the next assignment.

Top-level `val` remains subject to the current runtime limitation that package
bindings are mutable. The checker nevertheless rejects source-level
reassignment of an inferred/declared `val` in the same typing session. Fixing
package storage semantics is separate work.

### Literals and Operators

- integer literals: `Long`;
- floating literals: `Double`;
- booleans, chars, strings, and null: their corresponding types;
- list literals: `List[join(elements)]`, or `List[Any]` when empty;
- map literals: `Map[join(keys), join(values)]`, or `Map[Any, Any]` when empty;
- ranges: `Array[Long]`;
- arithmetic and ordering validate known operands and infer the existing
  runtime result type;
- equality accepts all types and returns `Boolean`;
- logical operators require Boolean-compatible operands unless an operand is
  `Any`;
- indexing a known list/array/map returns its component type; dynamic or
  unknown receivers return `Any`.

### Control Flow

- conditions of `if`, loops, and ternaries must be `Boolean` or `Any`;
- an `if`/ternary/switch result is the join of its result branches, including
  `Null` when there is no `else`;
- a block/expression list has the type of its final expression, or `Unit` when
  empty;
- loop, import, package, record definition, and declaration-only forms use
  their existing result where meaningful and otherwise `Unit`/`Any`
  conservatively;
- `return`, `yield`, `break`, and exception paths contribute to their enclosing
  construct where the first slice can prove it, without introducing a general
  effect system.

### Functions

- an annotated parameter uses its annotation;
- an unannotated parameter is `Any`;
- an annotated return type constrains all explicit returns and the body result;
- an unannotated return type is inferred by joining explicit returns and the
  body result;
- named functions are predeclared before checking their body, so recursion
  works;
- unannotated recursive return cycles that cannot be solved locally fall back
  to `Any`;
- calls through a known `Function` validate arity and arguments and return the
  known return type;
- calls through `Any`, overloaded runtime groups, built-ins without signatures,
  or Java reflection return `Any`.

Existing generic annotations are represented using `TypeVariable`. The first
slice keeps the current shallow call-site binding behavior; it does not attempt
Hindley-Milner polymorphism or Java generic reflection.

### Dynamic Boundaries

These produce `Any` unless a surrounding annotation supplies an expected type:

- pre-existing package/global bindings without typing metadata;
- built-in and native functions without declared signatures;
- Java member access, method calls, constructors, casts not fully described by
  source annotations, and reflection;
- `eval(string)` as observed by the caller.

The code *inside* `eval(string)` and `load(path)` still goes through mandatory
typing before it runs. Dynamic is a boundary, not an escape hatch around the
checker.

## Architecture

### `spnuts.typing`

Add a shared package containing:

- `StaticType`: the semantic type algebra and display names;
- `TypeBinding`: type plus mutability;
- `TypeEnvironment`: lexical scope stack and top-level bindings;
- `TypingSession`: persistent top-level state for one execution context;
- `TypeTable`: expression-to-type overlay kept separate from the AST;
- `TypeChecker`: AST traversal, inference, compatibility checks, and
  diagnostics;
- `TypeError`: message, source position, expected type, and actual type where
  applicable.

The AST remains unchanged. A typed overlay avoids a second AST hierarchy and
keeps parsing, compilation, and interpretation decoupled. `TypeTable` is keyed
by expression identity rather than case-class structural equality so repeated
equal-looking expressions keep distinct source positions.

Checking produces a `TypingResult(table, nextEnvironment)`. It does not mutate
the live `TypingSession`. The caller commits `nextEnvironment` only after
evaluation succeeds. SPnuts execution is not transactional, so an expression
before a later runtime failure may already have changed the runtime package;
such an uncommitted binding is deliberately seen as `Any` on the next chunk
rather than pretending the failed chunk's static guarantees were established.

### Mandatory Interpreter Gate

`Context` owns a `TypingSession`. The public `Interpreter.eval` method is the
program-entry boundary; recursive AST evaluation is moved to the private
`evalInner` method.

At the outermost `Interpreter.eval(expr, ctx)`:

1. type-check the complete `expr` against a snapshot of `ctx.typingSession`;
2. if checking fails, throw `TypeError` before evaluating any node;
3. evaluate normally, retaining existing runtime checks;
4. commit the proposed typing environment only if evaluation succeeds.

Recursive calls made while walking the same AST use `evalInner` and do not
re-check individual nodes. A nested parsed program such as `eval` or `load`
calls the public `eval` entry again and is therefore checked as a complete
chunk, even when it shares the current `Context`.

This single gate covers:

- direct interpreter API use;
- the JVM and Native REPL;
- script-file execution through the REPL;
- `:load`;
- the `load()` built-in; and
- string `eval()`.

The JVM bytecode compiler invokes the same checker before code generation.
Unsupported code may still fall back to the interpreter, but a proven type
error may not fall through as a compilation failure.

### Session and Scope Behavior

Each `Context` gets its own `TypingSession`. A fresh session treats existing
runtime package/global values as `Any`; this preserves compatibility with
host-injected bindings and avoids JVM reflection in shared typing code.

Lexical scopes mirror interpreter scopes for blocks, loops, function
parameters, catches, and closures. Top-level definitions persist across
separate REPL `eval` calls. Failed checks and failed evaluations do not commit
new type bindings.

`cloneForEval()` creates a fresh typing session just as it creates a fresh
runtime package/import context. `load()` on the current context shares its
session and therefore contributes definitions to subsequent code.

## Diagnostics

`TypeError` is distinct from `RuntimeError` and carries `SourcePos`.

Messages name the binding or operation and include expected and actual types:

```text
Type error at <repl>:2:1: cannot assign String to 'x' (expected Long)
  x = "later"
  ^
```

The REPL catches `TypeError` and renders it with the existing source line/caret
formatter. CLI/script paths receive the same file, line, and column through the
exception message.

Errors point at the smallest useful expression:

- the initializer for declaration mismatch;
- the right-hand side for assignment mismatch;
- the argument for call mismatch;
- the returned expression for return mismatch;
- the operand for invalid operators/conditions.

## Runtime Checks

Static checking does not replace runtime enforcement:

- values crossing from `Any` to an annotated concrete type are still validated
  by the existing declaration/call/return machinery;
- Java interop retains its reflection-time checks;
- `Binding.staticType` continues to defend mutable runtime bindings.

The new `TypeTable` is diagnostic and semantic metadata in this slice; the
tree-walking interpreter does not need a new IR or inserted cast nodes.

## Testing

Tests are added in layers:

1. **Static type model**
   - display, normalization, compatibility, join, nullability, numeric widening.
2. **Type checker**
   - literal and collection inference;
   - fixed inferred binding types;
   - annotation constraints;
   - branch joins;
   - function parameter/return inference;
   - recursive predeclaration;
   - `Any` boundaries;
   - precise source positions and expected/actual diagnostics.
3. **Interpreter integration**
   - `x = 1; x = "later"` fails before an earlier side effect runs;
   - inferred state persists across successful calls with the same `Context`;
   - failed checks/evaluations do not commit type state;
   - `eval` and `load` cannot bypass checking;
   - existing runtime checks continue to catch concrete values from `Any`.
4. **REPL integration**
   - type errors include file:line:column, source line, and caret;
   - bindings and inferred types persist across `step`/`eval` calls;
   - a failed chunk leaves the session usable.
5. **Compiler integration**
   - proven-invalid ASTs are rejected before bytecode generation;
   - valid compilable and fallback programs keep working.

The complete JVM and Native suites must remain green.

## Documentation

Update both READMEs with:

- mandatory inference examples;
- annotation examples;
- the `Any` escape/boundary model;
- `val`/`var`/legacy assignment rules;
- `Long -> Double` widening and null behavior;
- examples of pre-execution diagnostics; and
- an explicit statement that there is no optional type-check mode.

## Out of Scope

- union/intersection types;
- flow-sensitive narrowing after `instanceof` or null checks;
- exhaustive switch analysis;
- full overload resolution and Java generic reflection;
- whole-program/module inference;
- Hindley-Milner generalization;
- changing package storage so top-level `val` is runtime-immutable;
- replacing all runtime `Class` checks with static types;
- compiler optimization based on `TypeTable`;
- a public type-query REPL command or language-server protocol.

Those can build on the semantic types and session boundary introduced here
without weakening the mandatory-checking contract.
