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
    // caret line is the last line; rendered as "  1 + )" (2-space indent),
    // so column 5 (1-indexed) lands '^' at string index 2 + 4 = 6
    lines.last.indexOf('^') shouldBe 6
  }

  it should "include file:line:column in runtime error output" in {
    val repl = freshRepl()
    val out  = repl.eval("null.foo()")
    out should include ("<repl>:1:5")
    out should include ("Runtime error")
  }

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

  it should "keep buffering across multiple lines until a function body closes" in {
    val repl = freshRepl()
    repl.step("function replTestColonBody(n) {") shouldBe StepResult.Continue
    repl.step("  n") shouldBe StepResult.Continue
    repl.step("}") shouldBe a [StepResult.Output]
  }
