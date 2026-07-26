package spnuts.typing

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spnuts.parser.Parser
import spnuts.typing.StaticType.*

class TypeCheckerSpec extends AnyFlatSpec with Matchers:
  private def check(code: String, env: TypeEnvironment = TypeEnvironment.empty) =
    TypeChecker.check(Parser.parse(code, "<test>"), env)

  "TypeChecker" should "infer and persist a legacy assignment type" in {
    val result = check("x = 1")
    result.nextEnvironment.lookup("x").map(_.tpe) shouldBe Some(LongType)
  }

  it should "reject an incompatible later assignment before execution" in {
    val error = intercept[TypeError] {
      check(
        """x = 1
          |x = "later"""".stripMargin
      )
    }
    error.expected shouldBe Some(LongType)
    error.actual shouldBe Some(StringType)
    error.pos.line shouldBe 2
  }

  it should "infer val and var initializer types and enforce annotations" in {
    check("val x = 1").nextEnvironment.lookup("x") shouldBe
      Some(TypeBinding(LongType, true))
    check("var x = 1").nextEnvironment.lookup("x") shouldBe
      Some(TypeBinding(LongType, false))
    val error = intercept[TypeError](check("""val x: Long = "no""""))
    error.expected shouldBe Some(LongType)
    error.actual shouldBe Some(StringType)
  }

  it should "reject source reassignment of val" in {
    intercept[TypeError](check("val x = 1; x = 2")).msg should include("immutable")
  }

  it should "infer primitive literal and arithmetic result types" in {
    check("1 + 2").resultType shouldBe LongType
    check("1 + 2.0").resultType shouldBe DoubleType
    check("true && false").resultType shouldBe BooleanType
  }

  it should "use a known global reference type" in {
    val environment =
      TypeEnvironment.empty.declare("answer", TypeBinding(LongType, false))

    check("::answer", environment).resultType shouldBe LongType
  }

  it should "reject a proven-invalid operator" in {
    val error = intercept[TypeError](check("""true - "x""""))
    error.pos.line shouldBe 1
  }
