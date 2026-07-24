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
