package spnuts.interpreter

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import spnuts.parser.Parser
import spnuts.runtime.{Context, JvmPlatform, PnutsPackage}

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
