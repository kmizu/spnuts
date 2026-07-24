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
