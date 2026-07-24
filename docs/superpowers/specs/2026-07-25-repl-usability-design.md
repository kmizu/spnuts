# REPL & Diagnostics Usability Improvements

Date: 2026-07-25
Status: Approved (self-approved under autonomous `/goal` session — see note below)

## Goal

`/goal`: "Without language changes, more usable language and toolset" — improve
how usable SPnuts is to work with *without touching the Pnuts language syntax
or semantics*. Scope is limited to the REPL and error diagnostics, which are
shared by the interactive REPL, script-file execution, and (indirectly) the
JSR-223 script engine.

Note on approval: this session runs under a `/goal`-driven Stop hook whose
directive is to act autonomously without pausing to ask the user what to do.
The brainstorming skill's normal approval gate (present design, wait for user
sign-off) is therefore short-circuited for this session: the design below is
self-approved based on concrete evidence gathered from the codebase, and
implementation proceeds immediately after this doc is committed.

## Problems found (evidence, not guesses)

1. **Source positions are computed but discarded.** `ParseError` and
   `RuntimeError` both carry a `SourcePos` (file:line:column) and bake it into
   their `getMessage`. But `Repl.eval` catches them and prints `e.message` /
   `e.msg` directly — the raw fields, not `getMessage` — so the position is
   silently dropped. A user gets `Parse error: Expected ')' but got Eof('')`
   with no indication of where.
2. **The interactive REPL cannot accept multi-line input at all.** `Main.scala`
   (JVM and Native) reads one line, evaluates it immediately, and repeats.
   Nothing detects "this statement isn't finished yet". This is confirmed by
   the README's own REPL transcript, which crams a full `if/else` function
   body onto one line to work around the limitation. Any multi-line function
   body, block, or `try/catch` typed interactively fails immediately with a
   parse error instead of prompting for the rest.
3. **No way to load a script into a running session** or **inspect what's
   been defined** interactively. Only `:help` and `:quit` exist today.

## Scope

In scope:
- Fix error display to include full position + a source-line/caret snippet.
- Multi-line continuation input in the interactive REPL (JVM + Native).
- `:load <path>` — evaluate a file into the current session.
- `:bindings` — list session-defined variables (built-ins excluded).

Out of scope (explicitly not doing, to avoid scope creep):
- Any change to Pnuts grammar, keywords, or evaluation semantics.
- Tab-completion / syntax highlighting (bigger, separate effort).
- Fixing the pre-existing unterminated-block-comment lexer leniency (silently
  consumes to EOF) — unrelated latent issue, not part of this pass.
- Cross-instance global-state isolation of `PnutsPackage.global` — pre-existing
  design, unrelated to usability of the REPL surface itself.

## Design

### 1. `ParseError.unexpectedEof`

Add a field to `ParseError` (default `false`), set to `true` by
`ParseError.unexpected` exactly when the offending token's kind is `TK.Eof`.
This lets callers distinguish "ran out of input" (keep prompting) from a
genuine syntax error (report and stop). No other call site of `ParseError`
needs to change since the field defaults to `false`.

### 2. `Repl.formatError`

Replace the swallow-the-position catch clauses in `eval` with a formatter that
renders:

```
Parse error at <repl>:1:9: Expected ')' but got Eof('')
  1 + (2 *
        ^
```

i.e. `<kind> at <pos>: <message>`, followed by the offending source line (from
the code string already being evaluated — no extra state needed) and a caret
under the column. Falls back to just the header line if `pos.line` is out of
range for the given source (defensive, but exercised by tests).

### 3. Multi-line interactive input: `StepResult` / `Repl.step`

Move the "what does a fresh line of interactive input do" logic out of the
two near-duplicate platform `Main.scala` loops and into shared `Repl`, so JVM
and Native behave identically:

```scala
enum StepResult:
  case Continue           // incomplete; caller should read another line
  case Output(text: String)
  case Quit
```

`Repl` gains:
- `private var buffer: String` — accumulated incomplete input.
- `def prompt: String` — `"pnuts> "` when buffer is empty, `"..... "` while
  continuing.
- `def step(line: String): StepResult` — commands (`:quit`, `:help`, `:load`,
  `:bindings`) are only recognized when `buffer` is empty (so typing literal
  text starting with `:` inside a continued block is never misread as a
  command). Otherwise appends the line to the buffer, parses the buffer with
  `isIncomplete`, and either continues buffering or evaluates the full
  buffered chunk via `eval` and clears the buffer.
- `private def isIncomplete(code: String): Boolean` — attempts
  `Parser.parse(code, "<repl>")`; returns `true` only when it fails with a
  `ParseError` whose `unexpectedEof` is `true`. Any other failure (or success)
  returns `false`, so genuine syntax errors are reported immediately rather
  than hanging the prompt forever.

`eval(code: String): String` keeps its current single-shot contract (used
as-is for whole-file script execution, where "incomplete" isn't a meaningful
concept — a malformed script file is just an error).

### 4. `:load <path>`

Reads the file (`scala.io.Source.fromFile`, already used by both platform
`Main.scala` files today for script-mode execution, so it's proven to work
under both JVM and Scala Native) and evaluates its contents via `eval` in the
current session's `ctx`, so definitions become available afterward. Errors
(missing file, parse/runtime error in the loaded file) are reported the same
way as any other REPL error, not as an unhandled exception.

### 5. `:bindings`

`Repl` snapshots `PnutsPackage.global.allBindings.map(_._1).toSet` at
construction time (before the user types anything). `:bindings` diffs the
current bindings against that snapshot and prints only what the user defined
this session, sorted, as `name = value`. Reuses the `allBindings` accessor
already added to `PnutsPackage` (currently used only by the JSR-223 script
engine bridge) rather than adding a second way to enumerate a package.

### 6. Platform `Main.scala` loops

Both JVM and Native interactive loops become thin: read a line using
`repl.prompt`, call `repl.step(line)`, match on `StepResult` (print output,
loop again on `Continue`, stop on `Quit`). Script-file mode (`args.nonEmpty`)
is untouched — it already calls `repl.eval` directly.

## Testing

New `ReplSpec` (shared, JVM-run since Native testing isn't part of the
existing CI loop for this repo) covering:
- Single-line eval unchanged (existing behavior).
- `formatError` includes file:line:column and a caret line pointing at the
  right column for both parse and runtime errors.
- `step()` returns `Continue` for an unterminated `{`/`(`/`[`, and completes
  correctly once the closing token arrives across multiple `step()` calls.
- A genuine syntax error (not an EOF issue) returns `Output` immediately
  rather than `Continue`.
- `:bindings` is empty right after construction, and shows a variable after
  it's defined via `step`, but never shows pre-existing built-ins.
- `:load` on a real temp file evaluates its contents into the session (a
  variable defined in the loaded file is visible afterward via `:bindings`
  or a follow-up expression); `:load` on a missing file reports an error
  instead of throwing.

All 430 existing tests must remain green.

## Docs

`README.md` / `README-ja.md`: update the REPL transcript to show a realistic
multi-line function definition, and extend the REPL commands table with
`:load` and `:bindings`.
