package spnuts.interpreter

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import spnuts.parser.Parser
import spnuts.runtime.{Context, JvmPlatform, PnutsPackage}
import spnuts.typing.{StaticType, TypeError}

class InterpreterJvmSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll:

  override def beforeAll(): Unit = JvmPlatform.init()

  private def run(src: String): Any =
    val pkg = PnutsPackage("interpolationTest", Some(PnutsPackage.global))
    Interpreter.eval(Parser.parse(src, "<test>"), Context(currentPackage = pkg))

  "Interpreter on the JVM" should "interpolate a Java method call result" in {
    run(""""upper: \("hello".toUpperCase())""") shouldBe "upper: HELLO"
  }

  it should "interpolate a nested Java method call" in {
    run("""s = "hello"; "len=\(s.length())""") shouldBe "len=5"
  }

  it should "support generic recursion over a Java list" in {
    run("""
      function lenFrom<T>(xs: List<T>, i: Long): Long {
        if (i >= xs.size()) 0 else 1 + lenFrom(xs, i + 1)
      }
      function len<T>(xs: List<T>): Long { lenFrom(xs, 0) }
      import java.util.*
      len(toList([1, 2, 3, 4, 5]))
    """) shouldBe 5L
  }

  it should "type-check loaded code before its side effects" in {
    val path = java.nio.file.Files.createTempFile("spnuts-typing-load", ".pnuts")
    try
      java.nio.file.Files.writeString(path, """loadSideEffect = 1; x = 1; x = "bad"""")
      val pkg = PnutsPackage("typingLoadTest", Some(PnutsPackage.global))
      val ctx = Context(currentPackage = pkg)
      val escapedPath = path.toAbsolutePath.toString.replace("\\", "\\\\")

      val error = intercept[TypeError] {
        Interpreter.eval(Parser.parse(s"""load("$escapedPath")""", "<load-test>"), ctx)
      }

      error.expected shouldBe Some(StaticType.LongType)
      val localBindings = pkg.allBindings.toMap
      localBindings.get("loadSideEffect") shouldBe None
      localBindings.get("x") shouldBe None
    finally
      java.nio.file.Files.deleteIfExists(path)
  }
