# REPL & Diagnostics Usability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the SPnuts REPL and its error diagnostics usable for real interactive work — multi-line input, position-aware error messages, `:load`, `:bindings` — without changing the Pnuts language itself.

**Architecture:** All new logic lives in the shared `Repl` class (`repl/shared`) so JVM and Native behave identically; the two platform `Main.scala` files shrink to a thin read/step/print loop. A new `unexpectedEof` flag on `ParseError` (core, shared) is the single signal the REPL uses to decide "keep reading" vs. "report this error now."

**Tech Stack:** Scala 3.3.1, ScalaTest 3.2.19, sbt cross-project (JVM + Scala Native), JLine3 (JVM REPL only).

## Global Constraints

- No changes to Pnuts grammar, keywords, or evaluation semantics (per `/goal`: "without language changes").
- Design source of truth: `docs/superpowers/specs/2026-07-25-repl-usability-design.md`.
- All 430 pre-existing tests must stay green throughout.
- Follow user's global testing rule: tests first (RED), then minimal implementation (GREEN), for every behavioral change.
- `repl` sbt project currently has **no test dependency and is not in CI** — Task 2 adds both.

---

### Task 1: `ParseError.unexpectedEof`

**Files:**
- Modify: `core/shared/src/main/scala/spnuts/parser/ParseError.scala`
- Test: `core/shared/src/test/scala/spnuts/parser/ParserSpec.scala` (append to existing file)

**Interfaces:**
- Produces: `ParseError(message: String, pos: SourcePos, unexpectedEof: Boolean = false)` — new 3rd field, default `false` so no other call site needs to change. `ParseError.unexpected(expected: String, got: Token): ParseError` now sets `unexpectedEof = got.kind == TokenKind.Eof`.

- [ ] **Step 1: Write the failing tests**

Append to `core/shared/src/test/scala/spnuts/parser/ParserSpec.scala` (add `import spnuts.parser.TokenKind` is unnecessary — file already lives in package `spnuts.parser`; just use `TokenKind` and `Token` unqualified):

```scala
  "ParseError.unexpected" should "set unexpectedEof=true when the offending token is Eof" in {
    val eofTok = Token(TokenKind.Eof, "", SourcePos("<test>", 1, 1))
    val err = ParseError.unexpected(")", eofTok)
    err.unexpectedEof shouldBe true
  }

  it should "set unexpectedEof=false when the offending token is not Eof" in {
    val tok = Token(TokenKind.RBrace, "}", SourcePos("<test>", 1, 1))
    val err = ParseError.unexpected(")", tok)
    err.unexpectedEof shouldBe false
  }

  it should "propagate unexpectedEof=true from Parser.parse on truncated input" in {
    val ex = intercept[ParseError] {
      Parser.parse("if (true) {", "<test>")
    }
    ex.unexpectedEof shouldBe true
  }

  it should "propagate unexpectedEof=false from Parser.parse on a genuine syntax error" in {
    val ex = intercept[ParseError] {
      Parser.parse("}", "<test>")
    }
    ex.unexpectedEof shouldBe false
  }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `sbt "coreJVM/testOnly spnuts.parser.ParserSpec"`
Expected: compile error (`unexpectedEof is not a member of ParseError`) — this is the RED state.

- [ ] **Step 3: Implement**

Replace the full contents of `core/shared/src/main/scala/spnuts/parser/ParseError.scala` with:

```scala
package spnuts.parser

import spnuts.ast.SourcePos

/**
 * Parse error with source position.
 *
 * @param unexpectedEof true when parsing failed only because input ran out
 *                       (e.g. an unclosed `{`), as opposed to a genuine
 *                       syntax error. Used by the REPL to decide whether to
 *                       keep reading more lines instead of reporting.
 */
case class ParseError(message: String, pos: SourcePos, unexpectedEof: Boolean = false)
  extends Exception(s"$pos: $message")

object ParseError:
  def unexpected(expected: String, got: Token): ParseError =
    ParseError(
      s"Expected $expected but got ${got.kind}('${got.image}')",
      got.pos,
      unexpectedEof = got.kind == TokenKind.Eof,
    )
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `sbt "coreJVM/testOnly spnuts.parser.ParserSpec"`
Expected: PASS, all tests in the suite green (including pre-existing ones — this is a backward-compatible field addition).

- [ ] **Step 5: Run the full JVM suite to make sure nothing else broke**

Run: `sbt "coreJVM/test"`
Expected: 434 tests passed (430 pre-existing + 4 new), 0 failed.

- [ ] **Step 6: Commit**

```bash
git add core/shared/src/main/scala/spnuts/parser/ParseError.scala core/shared/src/test/scala/spnuts/parser/ParserSpec.scala
git commit -m "feat: add ParseError.unexpectedEof for REPL incomplete-input detection"
```

---

### Task 2: Test infrastructure for the `repl` module

The `repl` sbt project has no test dependency configured and isn't run in CI. This task adds both, plus one baseline `ReplSpec` test (current single-line eval behavior, unchanged) to prove the wiring works before any new REPL behavior is added. It also makes `Repl`'s `Context` injectable so tests can use an isolated package instead of the shared `PnutsPackage.global` (mirrors the existing pattern in `core/jvm/src/test/scala/spnuts/compiler/CompilerSpec.scala`, which creates a child package per test rather than touching `PnutsPackage.global` directly).

**Files:**
- Modify: `build.sbt`
- Modify: `.github/workflows/ci.yml`
- Modify: `repl/shared/src/main/scala/spnuts/repl/Repl.scala`
- Test: `repl/shared/src/test/scala/spnuts/repl/ReplSpec.scala` (new)

**Interfaces:**
- Produces: `class Repl(val ctx: Context = Context())` — `ctx` becomes a constructor parameter (was a `val` initialized inline to the same default), so tests can pass an isolated `Context`. Existing call sites `Repl()` in both `Main.scala` files are unaffected (same default).

- [ ] **Step 1: Add scalatest to the `repl` crossProject and register CI jobs**

In `build.sbt`, change the `repl` project definition from:

```scala
lazy val repl = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("repl"))
  .dependsOn(core)
  .settings(name := "spnuts-repl")
```

to:

```scala
lazy val repl = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("repl"))
  .dependsOn(core)
  .settings(
    name := "spnuts-repl",
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.19" % Test,
  )
```

In `.github/workflows/ci.yml`, add a step to the end of the `test-jvm` job (after "Run JVM tests"):

```yaml
      - name: Run REPL JVM tests
        run: sbt "replJVM/test"
```

and to the end of the `test-native` job (after "Run Scala Native tests"):

```yaml
      - name: Run REPL Native tests
        run: sbt "replNative/test"
```

- [ ] **Step 2: Make `Repl`'s context injectable**

In `repl/shared/src/main/scala/spnuts/repl/Repl.scala`, change:

```scala
class Repl:
  val ctx: Context = Context()
  ctx.writer.println(banner)
```

to:

```scala
class Repl(val ctx: Context = Context()):
  ctx.writer.println(banner)
```

- [ ] **Step 3: Write the failing baseline test**

Create `repl/shared/src/test/scala/spnuts/repl/ReplSpec.scala`:

```scala
package spnuts.repl

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spnuts.runtime.{Context, PnutsPackage}

class ReplSpec extends AnyFlatSpec with Matchers:

  /** Fresh, isolated Repl — its own child package, so tests never see
    * bindings left over by other tests or by PnutsPackage.global. */
  private var counter = 0
  def freshRepl(): Repl =
    counter += 1
    val pkg = PnutsPackage(s"replTest$counter", Some(PnutsPackage.global))
    val ctx = Context(currentPackage = pkg, writer = new java.io.PrintWriter(java.io.Writer.nullWriter()))
    Repl(ctx)

  "Repl.eval" should "evaluate a simple expression and format the result" in {
    val repl = freshRepl()
    repl.eval("1 + 2") shouldBe "3"
  }

  it should "return an empty string for blank input" in {
    val repl = freshRepl()
    repl.eval("") shouldBe ""
    repl.eval("   ") shouldBe ""
  }
```

- [ ] **Step 4: Run the test**

This task adds test *infrastructure*, not new REPL behavior, so there's no
behavioral RED state to observe — `Repl.eval` itself doesn't change in this
task. The meaningful check is that the new `repl` test setup (Step 1) and the
constructor change (Step 2) actually wire up correctly end-to-end.

Run: `sbt "replJVM/testOnly spnuts.repl.ReplSpec"`
Expected: PASS, 2 tests succeeded. If it fails to compile or run, the cause is
infra wiring (most likely: a typo in the `build.sbt` dependency, or the
`crossType(CrossType.Full)` / `repl/shared/src/test` source layout not being
picked up) — fix that before continuing; don't change `Repl.scala`'s
behavior to work around it.

- [ ] **Step 5: Run full suites to confirm nothing regressed**

Run: `sbt "coreJVM/test" "replJVM/test"`
Expected: all green (434 core + 2 repl).

- [ ] **Step 6: Commit**

```bash
git add build.sbt .github/workflows/ci.yml repl/shared/src/main/scala/spnuts/repl/Repl.scala repl/shared/src/test/scala/spnuts/repl/ReplSpec.scala
git commit -m "test: add scalatest + CI wiring for repl module, make Repl.ctx injectable"
```

---

### Task 3: `formatError` — position-aware error messages

**Files:**
- Modify: `repl/shared/src/main/scala/spnuts/repl/Repl.scala`
- Test: `repl/shared/src/test/scala/spnuts/repl/ReplSpec.scala`

**Interfaces:**
- Consumes: `ParseError(message, pos, unexpectedEof)` and `RuntimeError(msg, pos, cause)` from Task 1 / existing `core`.
- Produces: `Repl.eval` error output format `"$kind at $pos: $message\n  $sourceLine\n  $caret"`.

- [ ] **Step 1: Write the failing tests**

Append to `ReplSpec.scala`:

```scala
  it should "include file:line:column and a caret in parse error output" in {
    val repl = freshRepl()
    val out  = repl.eval("1 + )")
    out should include ("<repl>:1:5")
    out should include ("1 + )")
    out should include ("^")
  }

  it should "point the caret at the correct column for a multi-line buffer" in {
    val repl = freshRepl()
    val out  = repl.eval("x = 1\n1 + )")
    out should include ("<repl>:2:5")
    val lines = out.split("\n")
    // caret line is the last line; its position of '^' must match column 5 (1-indexed)
    // rendered as "  1 + )" (2-space indent) so '^' lands at index 2 + 4 = 6
    lines.last.indexOf('^') shouldBe 6
  }

  it should "include file:line:column in runtime error output" in {
    val repl = freshRepl()
    val out  = repl.eval("undefinedVariableXyz")
    out should include ("<repl>:1:1")
    out should include ("Runtime error")
  }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `sbt "replJVM/testOnly spnuts.repl.ReplSpec"`
Expected: FAIL on the three new tests — current output is `"Parse error: Expected ... but got ..."` / `"Runtime error: Undefined variable: ..."` with no position substring.

- [ ] **Step 3: Implement `formatError` and wire it into `eval`**

In `repl/shared/src/main/scala/spnuts/repl/Repl.scala`, replace the `eval` method's catch block:

```scala
        catch
          case e: ParseError   => s"Parse error: ${e.message}"
          case e: RuntimeError => s"Runtime error: ${e.msg}"
          case e: Throwable    => s"Error: ${e.getMessage}"
```

with:

```scala
        catch
          case e: ParseError   => formatError("Parse error", e.pos, e.message, code)
          case e: RuntimeError => formatError("Runtime error", e.pos, e.msg, code)
          case e: Throwable    => s"Error: ${e.getMessage}"
```

and add a new private method (place it after `formatResult`):

```scala
  private def formatError(kind: String, pos: SourcePos, msg: String, source: String): String =
    val header = s"$kind at $pos: $msg"
    val lines  = source.split("\n", -1)
    if pos.line >= 1 && pos.line <= lines.length then
      val srcLine = lines(pos.line - 1)
      val caret   = " " * math.max(0, pos.column - 1) + "^"
      s"$header\n  $srcLine\n  $caret"
    else header
```

Add the import `spnuts.ast.SourcePos` at the top of the file if not already present (it already is, per the existing `import spnuts.ast.SourcePos` line).

- [ ] **Step 4: Run the tests to verify they pass**

Run: `sbt "replJVM/testOnly spnuts.repl.ReplSpec"`
Expected: PASS, all tests in the suite green.

- [ ] **Step 5: Run full suites**

Run: `sbt "coreJVM/test" "replJVM/test"`
Expected: all green.

- [ ] **Step 6: Commit**

```bash
git add repl/shared/src/main/scala/spnuts/repl/Repl.scala repl/shared/src/test/scala/spnuts/repl/ReplSpec.scala
git commit -m "fix: REPL error messages include file:line:column and a caret snippet"
```

---

### Task 4: Multi-line input — `StepResult` and `Repl.step`

**Files:**
- Modify: `repl/shared/src/main/scala/spnuts/repl/Repl.scala`
- Test: `repl/shared/src/test/scala/spnuts/repl/ReplSpec.scala`

**Interfaces:**
- Produces:
  - `enum StepResult { case Continue; case Output(text: String); case Quit }`
  - `def Repl.prompt: String`
  - `def Repl.step(line: String): StepResult`
- Consumes: `ParseError.unexpectedEof` (Task 1), `Repl.eval` (existing/Task 3).

- [ ] **Step 1: Write the failing tests**

Append to `ReplSpec.scala`:

```scala
  "Repl.step" should "evaluate a complete single line immediately" in {
    val repl = freshRepl()
    repl.step("40 + 2") shouldBe StepResult.Output("42")
    repl.prompt shouldBe "pnuts> "
  }

  it should "return Continue for an unclosed brace and switch to the continuation prompt" in {
    val repl = freshRepl()
    repl.step("if (true) {") shouldBe StepResult.Continue
    repl.prompt shouldBe "..... "
  }

  it should "complete a multi-line if-block across multiple step calls" in {
    val repl = freshRepl()
    repl.step("if (true) {") shouldBe StepResult.Continue
    repl.step("  99") shouldBe StepResult.Continue
    repl.step("}") shouldBe StepResult.Output("99")
    repl.prompt shouldBe "pnuts> "
  }

  it should "report a genuine syntax error immediately instead of buffering forever" in {
    val repl = freshRepl()
    val result = repl.step("1 + )")
    result shouldBe a [StepResult.Output]
    val StepResult.Output(text) = result: @unchecked
    text should include ("Parse error")
    repl.prompt shouldBe "pnuts> "
  }

  it should "return Quit for :quit, :exit, and :q" in {
    freshRepl().step(":quit") shouldBe StepResult.Quit
    freshRepl().step(":exit") shouldBe StepResult.Quit
    freshRepl().step(":q") shouldBe StepResult.Quit
  }

  it should "not treat a line starting with ':' as a command while mid-continuation" in {
    val repl = freshRepl()
    repl.step("function replTestColonBody(n) {") shouldBe StepResult.Continue
    // ':' has no special meaning inside a buffered block; this line is just
    // more code and (being invalid Pnuts) surfaces as part of the eventual
    // parse — here we close the block instead to keep the test deterministic.
    repl.step("  n") shouldBe StepResult.Continue
    repl.step("}") shouldBe a [StepResult.Output]
  }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `sbt "replJVM/testOnly spnuts.repl.ReplSpec"`
Expected: compile error (`StepResult`/`step`/`prompt` don't exist yet) — RED.

- [ ] **Step 3: Implement**

In `repl/shared/src/main/scala/spnuts/repl/Repl.scala`, add before the `class Repl` definition:

```scala
/** Result of feeding one line of interactive input to a Repl. */
enum StepResult:
  case Continue
  case Output(text: String)
  case Quit
```

Inside `class Repl`, add a mutable buffer field and the `prompt`/`step`/`isIncomplete` members (place `prompt` and `step` right after the constructor's `ctx.writer.println(banner)` line, and `isIncomplete` as a private helper near `formatError`):

```scala
  private var buffer: String = ""

  /** Prompt to show for the next line of input. */
  def prompt: String = if buffer.isEmpty then "pnuts> " else "..... "

  /**
   * Feed one line of interactive input. Commands (`:quit`, `:help`, ...) are
   * only recognized when not in the middle of a multi-line statement.
   * Incomplete statements are buffered across calls until a full statement
   * can be parsed, at which point it's evaluated via `eval`.
   */
  def step(line: String): StepResult =
    if buffer.isEmpty then
      line.trim match
        case ":quit" | ":exit" | ":q" => return StepResult.Quit
        case ":help"                  => return StepResult.Output(helpText)
        case ""                       => return StepResult.Output("")
        case _                        => ()

    val candidate = if buffer.isEmpty then line else buffer + "\n" + line
    if isIncomplete(candidate) then
      buffer = candidate
      StepResult.Continue
    else
      buffer = ""
      StepResult.Output(eval(candidate))
```

and, as a private helper (near `formatError`):

```scala
  /** True if `code` fails to parse only because it ran out of input. */
  private def isIncomplete(code: String): Boolean =
    try
      Parser.parse(code, "<repl>")
      false
    catch
      case e: ParseError => e.unexpectedEof
      case _: Throwable  => false
```

Update `helpText` to mention the new behavior (final text finalized in Task 5/6 once `:load`/`:bindings` exist — for this task, just keep the existing two-line help text as-is; it will be extended in Task 6).

- [ ] **Step 4: Run the tests to verify they pass**

Run: `sbt "replJVM/testOnly spnuts.repl.ReplSpec"`
Expected: PASS, all tests green.

- [ ] **Step 5: Run full suites**

Run: `sbt "coreJVM/test" "replJVM/test"`
Expected: all green.

- [ ] **Step 6: Commit**

```bash
git add repl/shared/src/main/scala/spnuts/repl/Repl.scala repl/shared/src/test/scala/spnuts/repl/ReplSpec.scala
git commit -m "feat: multi-line interactive input via Repl.step/StepResult"
```

---

### Task 5: `:load <path>` command

Note on test placement: this is the one behavior that touches real file I/O.
The rest of `Repl` lives in `repl/shared` and is tested there so both JVM and
Native get identical coverage, but this codebase's existing convention is to
keep file I/O JVM-only in *tests* even when the production code path is
shared (e.g. `core/jvm` has its own `JavaInteropSpec`/`ScriptEngineSpec`
rather than putting JVM-`java.nio.file` usage in `core/shared/src/test`).
`:load`'s implementation itself stays in shared `Repl.scala` (it only uses
`scala.io.Source.fromFile`, already proven cross-platform by both existing
`Main.scala` files' script-mode), but its temp-file-based tests go in a
JVM-only spec to avoid depending on `java.nio.file.Files`/`java.io.File`
temp-file behavior under Scala Native, which isn't exercised anywhere else
in this codebase.

**Files:**
- Modify: `repl/shared/src/main/scala/spnuts/repl/Repl.scala`
- Test: `repl/jvm/src/test/scala/spnuts/repl/ReplLoadSpec.scala` (new)

**Interfaces:**
- Consumes: `Repl.eval` (Task 3), `Repl.step` (Task 4).
- Produces: `:load <path>` recognized by `step` when `buffer.isEmpty`.

- [ ] **Step 1: Write the failing tests**

Create `repl/jvm/src/test/scala/spnuts/repl/ReplLoadSpec.scala`:

```scala
package spnuts.repl

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spnuts.runtime.{Context, PnutsPackage}

class ReplLoadSpec extends AnyFlatSpec with Matchers:

  private var counter = 0
  def freshRepl(): Repl =
    counter += 1
    val pkg = PnutsPackage(s"replLoadTest$counter", Some(PnutsPackage.global))
    val ctx = Context(currentPackage = pkg, writer = new java.io.PrintWriter(java.io.Writer.nullWriter()))
    Repl(ctx)

  ":load" should "evaluate a script file's contents into the session" in {
    val repl = freshRepl()
    val tmp  = java.io.File.createTempFile("replspec", ".pnuts")
    tmp.deleteOnExit()
    java.nio.file.Files.writeString(tmp.toPath, "replTestLoadedVar = 7\n")
    repl.step(s":load ${tmp.getAbsolutePath}") shouldBe StepResult.Output("")
    repl.eval("replTestLoadedVar") shouldBe "7"
  }

  it should "report a clean error when the path doesn't exist" in {
    val repl = freshRepl()
    val result = repl.step(":load /no/such/file/replspec-missing.pnuts")
    result shouldBe a [StepResult.Output]
    val StepResult.Output(text) = result: @unchecked
    text should include ("Error loading")
  }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `sbt "replJVM/testOnly spnuts.repl.ReplLoadSpec"`
Expected: FAIL — `:load ...` currently falls through to `eval(":load ...")`, which is a parse error, not the expected behavior.

- [ ] **Step 3: Implement**

In `Repl.step`, add a case for `:load` alongside the existing command cases:

```scala
        case ":quit" | ":exit" | ":q" => return StepResult.Quit
        case ":help"                  => return StepResult.Output(helpText)
        case cmd if cmd.startsWith(":load ") =>
          return StepResult.Output(loadFile(cmd.stripPrefix(":load ").trim))
        case ""                       => return StepResult.Output("")
        case _                        => ()
```

Add the private helper near `isIncomplete`:

```scala
  private def loadFile(path: String): String =
    try
      val content = scala.io.Source.fromFile(path).mkString
      eval(content)
    catch
      case e: Throwable => s"Error loading '$path': ${e.getMessage}"
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `sbt "replJVM/testOnly spnuts.repl.ReplLoadSpec"`
Expected: PASS, all tests green.

- [ ] **Step 5: Run full suites**

Run: `sbt "coreJVM/test" "replJVM/test"`
Expected: all green.

- [ ] **Step 6: Commit**

```bash
git add repl/shared/src/main/scala/spnuts/repl/Repl.scala repl/jvm/src/test/scala/spnuts/repl/ReplLoadSpec.scala
git commit -m "feat: add :load <path> REPL command"
```

---

### Task 6: `:bindings` command

**Files:**
- Modify: `repl/shared/src/main/scala/spnuts/repl/Repl.scala`
- Test: `repl/shared/src/test/scala/spnuts/repl/ReplSpec.scala`

**Interfaces:**
- Consumes: `PnutsPackage.allBindings: Iterable[(String, Binding)]` (already exists, currently used only by the JSR-223 bridge).
- Produces: `:bindings` recognized by `step` when `buffer.isEmpty`.

- [ ] **Step 1: Write the failing tests**

Append to `ReplSpec.scala`:

```scala
  ":bindings" should "report no user-defined bindings right after construction" in {
    val repl = freshRepl()
    repl.step(":bindings") shouldBe StepResult.Output("(no user-defined bindings)")
  }

  it should "list a variable after it's defined, but never pre-existing built-ins" in {
    val repl = freshRepl()
    repl.step("replTestBindingsVar = 5")
    val result = repl.step(":bindings")
    result shouldBe a [StepResult.Output]
    val StepResult.Output(text) = result: @unchecked
    text should include ("replTestBindingsVar = 5")
    text should not include ("println") // a built-in, must not show up
  }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `sbt "replJVM/testOnly spnuts.repl.ReplSpec"`
Expected: FAIL — `:bindings` currently falls through to `eval(":bindings")`, a parse error.

- [ ] **Step 3: Implement**

In `class Repl`, add a snapshot field right after the class parameter list (before `ctx.writer.println(banner)`):

```scala
  private val initialBindingNames: Set[String] =
    ctx.currentPackage.allBindings.map(_._1).toSet
```

Add a case in `step`'s command match:

```scala
        case ":quit" | ":exit" | ":q" => return StepResult.Quit
        case ":help"                  => return StepResult.Output(helpText)
        case ":bindings"              => return StepResult.Output(bindingsText)
        case cmd if cmd.startsWith(":load ") =>
          return StepResult.Output(loadFile(cmd.stripPrefix(":load ").trim))
        case ""                       => return StepResult.Output("")
        case _                        => ()
```

Add the private helper near `loadFile`:

```scala
  private def bindingsText: String =
    val userDefined = ctx.currentPackage.allBindings
      .filterNot((name, _) => initialBindingNames.contains(name))
      .toSeq
      .sortBy(_._1)
    if userDefined.isEmpty then "(no user-defined bindings)"
    else userDefined.map((name, b) => s"$name = ${formatResult(b.value)}").mkString("\n")
```

Finally, update `helpText` to document all four commands:

```scala
  private def helpText: String =
    """:help      — this message
      |:quit      — exit REPL
      |:load PATH — evaluate a script file into this session
      |:bindings  — list variables defined in this session
      |Any Pnuts expression is evaluated and the result printed.""".stripMargin
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `sbt "replJVM/testOnly spnuts.repl.ReplSpec"`
Expected: PASS, all tests green.

- [ ] **Step 5: Run full suites**

Run: `sbt "coreJVM/test" "replJVM/test"`
Expected: all green.

- [ ] **Step 6: Commit**

```bash
git add repl/shared/src/main/scala/spnuts/repl/Repl.scala repl/shared/src/test/scala/spnuts/repl/ReplSpec.scala
git commit -m "feat: add :bindings REPL command"
```

---

### Task 7: Wire the JVM interactive loop to `step`/`prompt`

**Files:**
- Modify: `repl/jvm/src/main/scala/spnuts/repl/Main.scala`

**Interfaces:**
- Consumes: `Repl.prompt`, `Repl.step(line): StepResult`, `StepResult.{Continue, Output, Quit}` (Task 4).

No new automated test — this file is a thin `main` entry point exercised via manual verification in Step 2 below (this project has no existing precedent for testing `Main.main` directly, and `Repl` itself, which holds all the logic, is already fully covered by `ReplSpec`).

- [ ] **Step 1: Replace the file contents**

Replace `repl/jvm/src/main/scala/spnuts/repl/Main.scala` in full with:

```scala
package spnuts.repl

import spnuts.runtime.JvmPlatform
import org.jline.reader.{LineReaderBuilder, EndOfFileException, UserInterruptException}
import org.jline.terminal.TerminalBuilder

/**
 * JVM REPL entry point with JLine3 for readline support.
 */
object Main:
  def main(args: Array[String]): Unit =
    JvmPlatform.init()
    val repl = Repl()

    if args.nonEmpty then
      // Script mode: evaluate file
      try
        val src = scala.io.Source.fromFile(args(0)).mkString
        val result = repl.eval(src)
        if result.nonEmpty then println(result)
      catch
        case e: java.io.FileNotFoundException => println(s"Error: ${e.getMessage}")
    else
      // Interactive mode
      val terminal = TerminalBuilder.terminal()
      val reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .variable(org.jline.reader.LineReader.HISTORY_FILE, ".spnuts_history")
        .build()

      var running = true
      while running do
        try
          val line = reader.readLine(repl.prompt)
          if line != null then
            repl.step(line) match
              case StepResult.Continue    => ()
              case StepResult.Output(txt) => if txt.nonEmpty then println(txt)
              case StepResult.Quit        => running = false
        catch
          case _: EndOfFileException     => running = false
          case _: UserInterruptException => running = false
          case e: Throwable              => println(s"Error: ${e.getMessage}")

      terminal.close()
```

- [ ] **Step 2: Manually verify the interactive loop**

Run: `sbt "replJVM/run"`, then at the `pnuts>` prompt type:

```
if (true) {
```

Expected: prompt changes to `..... `. Then type `42` and `}`:

```
..... 42
..... }
```

Expected: prints `42`, prompt returns to `pnuts> `. Then type `:bindings`, `:load somefile.pnuts` (any path), `:quit` to confirm each still works, then exit with Ctrl-D.

- [ ] **Step 3: Run full suites**

Run: `sbt "coreJVM/test" "replJVM/test"`
Expected: all green (this task has no new automated tests, so counts are unchanged from Task 6).

- [ ] **Step 4: Commit**

```bash
git add repl/jvm/src/main/scala/spnuts/repl/Main.scala
git commit -m "feat: JVM REPL uses step()/prompt for multi-line input and clean file errors"
```

---

### Task 8: Wire the Native interactive loop to `step`/`prompt`

**Files:**
- Modify: `repl/native/src/main/scala/spnuts/repl/Main.scala`

**Interfaces:**
- Consumes: same as Task 7.

- [ ] **Step 1: Replace the file contents**

Replace `repl/native/src/main/scala/spnuts/repl/Main.scala` in full with:

```scala
package spnuts.repl

/**
 * Scala Native REPL entry point.
 * Uses simple stdin readline for now (no readline library dependency).
 */
object Main:
  def main(args: Array[String]): Unit =
    val repl = Repl()

    if args.nonEmpty then
      try
        val src = scala.io.Source.fromFile(args(0)).mkString
        val result = repl.eval(src)
        if result.nonEmpty then println(result)
      catch
        case e: java.io.FileNotFoundException => println(s"Error: ${e.getMessage}")
    else
      var running = true
      while running do
        print(repl.prompt)
        Console.flush()
        val line = scala.io.StdIn.readLine()
        if line == null then
          running = false
        else
          try
            repl.step(line) match
              case StepResult.Continue    => ()
              case StepResult.Output(txt) => if txt.nonEmpty then println(txt)
              case StepResult.Quit        => running = false
          catch
            case e: Throwable => println(s"Error: ${e.getMessage}")
```

- [ ] **Step 2: Run the Native test suite**

Run: `sbt "coreNative/test" "replNative/test"`
Expected: all green. (If the Scala Native toolchain isn't installed locally, this step runs in CI per Task 2's workflow change — note that in the final verification task and move on.)

- [ ] **Step 3: Commit**

```bash
git add repl/native/src/main/scala/spnuts/repl/Main.scala
git commit -m "feat: Native REPL uses step()/prompt for multi-line input and clean file errors"
```

---

### Task 9: Update README.md

**Files:**
- Modify: `README.md`

(`README-ja.md` is a condensed quickstart that never documented REPL commands in the first place — left as-is to avoid scope creep; it stays accurate, just less detailed.)

**Interfaces:** none (documentation only).

- [ ] **Step 1: Replace the REPL transcript**

In `README.md`, replace:

```
```
SPnuts 0.1-SNAPSHOT (Scala reimplementation)
Thanks to Tomatsu-san for the original Pnuts.
Type :quit to exit, :help for commands.
pnuts> 1 + 2
3
pnuts> function fib(n) if (n <= 1) n else fib(n-1) + fib(n-2)
pnuts> fib(10)
55
pnuts> :quit
```
```

with:

```
```
SPnuts 0.1-SNAPSHOT (Scala reimplementation)
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
```

- [ ] **Step 2: Replace the REPL commands table**

Replace:

```
| Command | Description |
|---------|-------------|
| `:help` | Show help |
| `:quit` / `:exit` / `:q` | Exit |
```

with:

```
| Command | Description |
|---------|-------------|
| `:help` | Show help |
| `:quit` / `:exit` / `:q` | Exit |
| `:load PATH` | Evaluate a script file into the current session |
| `:bindings` | List variables defined so far in this session |

Multi-line input is supported: if a statement is left open (an unclosed
`{`, `(`, or `[`), the prompt changes to `..... ` and keeps reading until
the statement is complete.
```

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: document multi-line REPL input, :load, and :bindings"
```

---

### Task 10: Final full-suite verification

**Files:** none (verification only).

- [ ] **Step 1: Run every test suite**

Run: `sbt "coreJVM/test" "replJVM/test" "coreNative/test" "replNative/test"`
Expected: all green. Record the final counts (should be 434 core-JVM tests, core-Native mirrors the shared subset, plus the new `ReplSpec` suite on both `repl` platforms).

- [ ] **Step 2: Confirm no stray uncommitted changes**

Run: `git status`
Expected: clean (everything from Tasks 1–9 committed). If the pre-existing uncommitted JSR-223 work (`CompilerSpec.scala`, `PnutsPackage.scala`, `core/jvm/src/main/scala/spnuts/script/`, `core/jvm/src/main/resources/`, `core/jvm/src/test/scala/spnuts/script/`) is still uncommitted, that's a separate, already-in-flight piece of work predating this plan — leave it for the user to commit separately unless they've asked otherwise.

- [ ] **Step 3: Report done**

Summarize what changed (multi-line REPL input, position-aware errors, `:load`, `:bindings`, CI now covers `repl`) and point at the design/plan docs.
