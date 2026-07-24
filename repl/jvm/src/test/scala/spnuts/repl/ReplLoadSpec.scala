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
    repl.step(s":load ${tmp.getAbsolutePath}") shouldBe StepResult.Output("7")
    repl.eval("replTestLoadedVar") shouldBe "7"
  }

  it should "report a clean error when the path doesn't exist" in {
    val repl = freshRepl()
    val result = repl.step(":load /no/such/file/replspec-missing.pnuts")
    result shouldBe a [StepResult.Output]
    val StepResult.Output(text) = result: @unchecked
    text should include ("Error loading")
  }
