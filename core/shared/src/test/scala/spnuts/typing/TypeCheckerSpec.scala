package spnuts.typing

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spnuts.ast.BinaryExpr
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

  it should "reject an incompatible first target in a scalar multi-assignment" in {
    val error = intercept[TypeError] {
      check("""var x: String = ""; x, y = 1""")
    }
    error.expected shouldBe Some(StringType)
    error.actual shouldBe Some(LongType)
  }

  it should "assign the scalar type to the first multi-target and Null to the rest" in {
    val result = check("x, y = 1")

    result.nextEnvironment.lookup("x") shouldBe Some(TypeBinding(LongType, false))
    result.nextEnvironment.lookup("y") shouldBe Some(TypeBinding(NullType, false))
  }

  it should "reject an incompatible remaining target in a scalar multi-assignment" in {
    val error = intercept[TypeError] {
      check("var y: Long = 0; x, y = 1")
    }
    error.expected shouldBe Some(LongType)
    error.actual shouldBe Some(NullType)
  }

  it should "reject Char arithmetic" in {
    intercept[TypeError](check("'a' + 1"))
  }

  it should "reject unary minus on Char" in {
    intercept[TypeError](check("-'a'"))
  }

  it should "reject increment and numeric compound assignment on Char" in {
    intercept[TypeError](check("var x = 'a'; x++"))
    intercept[TypeError](check("var x = 'a'; x += 1"))
  }

  it should "infer and validate compound assignments" in {
    val result = check("var x: Double = 1; x += 2")
    result.resultType shouldBe DoubleType
    result.nextEnvironment.lookup("x") shouldBe Some(TypeBinding(DoubleType, false))

    val error = intercept[TypeError](check("var x = 1; x += 2.0"))
    error.expected shouldBe Some(LongType)
    error.actual shouldBe Some(DoubleType)
  }

  it should "permit an Any operand when validity is not statically known" in {
    val environment =
      TypeEnvironment.empty.declare("dynamic", TypeBinding(AnyType, false))

    check("dynamic - 1", environment).resultType shouldBe AnyType
  }

  it should "record operator and operand types in the type table" in {
    val expression = Parser.parseExpr("1 + 2", "<test>")
    val result = TypeChecker.check(expression, TypeEnvironment.empty)

    result.table.get(expression) shouldBe Some(LongType)
    expression match
      case BinaryExpr(_, lhs, rhs, _) =>
        result.table.get(lhs) shouldBe Some(LongType)
        result.table.get(rhs) shouldBe Some(LongType)
      case _ => fail("parser did not produce a binary expression")
  }

  it should "reject a proven-invalid operator" in {
    val error = intercept[TypeError](check("""true - "x""""))
    error.pos.line shouldBe 1
  }
