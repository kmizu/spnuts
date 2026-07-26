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

  it should "infer homogeneous, widened, and empty collections" in {
    check("[1, 2]").resultType shouldBe ListType(LongType)
    check("[1, 2.0]").resultType shouldBe ListType(DoubleType)
    check("[]").resultType shouldBe ListType(AnyType)
    check("""{"x" => 1}""").resultType shouldBe MapType(StringType, LongType)
    TypeChecker.check(
      spnuts.ast.MapExpr(Nil, spnuts.ast.SourcePos("<test>", 1, 1)),
      TypeEnvironment.empty
    ).resultType shouldBe MapType(AnyType, AnyType)
  }

  it should "infer index component types" in {
    check("[1, 2][0]").resultType shouldBe LongType
    check("""{"x" => 1}["x"]""").resultType shouldBe LongType
  }

  it should "join branch types and require Boolean-compatible conditions" in {
    check("if (true) 1 else 2.0").resultType shouldBe DoubleType
    check("""if (true) 1 else "x"""").resultType shouldBe AnyType
    intercept[TypeError](check("if (1) 2 else 3"))
    noException should be thrownBy check("x = eval(\"1\"); if (x) 2 else 3")
  }

  it should "keep block-local loop and catch names out of the top level" in {
    val result = check("for (i : [1, 2]) { val local = i }; 0")
    result.nextEnvironment.lookup("i") shouldBe None
    result.nextEnvironment.lookup("local") shouldBe None
  }

  it should "infer ranges, slices, ternaries, switches, and missing branches" in {
    check("true ? 1 : 2.0").resultType shouldBe DoubleType
    check("""switch (1) { case 1: "x"; default: "y" }""").resultType shouldBe StringType
    check("""if (true) "x"""").resultType shouldBe StringType
    check("if (true) 1").resultType shouldBe AnyType

    val pos = spnuts.ast.SourcePos("<test>", 1, 1)
    val range = spnuts.ast.RangeExpr(
      spnuts.ast.IntLit(1, "1", pos),
      spnuts.ast.IntLit(3, "3", pos),
      pos
    )
    TypeChecker.check(range, TypeEnvironment.empty).resultType shouldBe ArrayType(LongType)

    val slice = spnuts.ast.RangeAccess(
      spnuts.ast.ListExpr(List(spnuts.ast.IntLit(1, "1", pos)), false, pos),
      spnuts.ast.IntLit(0, "0", pos),
      None,
      pos
    )
    TypeChecker.check(slice, TypeEnvironment.empty).resultType shouldBe ListType(LongType)
  }

  it should "validate collection indices and range bounds when their types are known" in {
    intercept[TypeError](check("""[1]["x"]"""))
    intercept[TypeError](check("""{"x" => 1}[1]"""))

    val pos = spnuts.ast.SourcePos("<test>", 1, 1)
    val range = spnuts.ast.RangeExpr(
      spnuts.ast.IntLit(1, "1", pos),
      spnuts.ast.StringLit("x", pos),
      pos
    )
    intercept[TypeError](TypeChecker.check(range, TypeEnvironment.empty))
  }

  it should "reject known non-indexable receivers at the receiver position" in {
    val receiverTypes = List(
      UnitType,
      BooleanType,
      LongType,
      DoubleType,
      CharType,
      NullType,
      FunctionType(Nil, AnyType)
    )

    receiverTypes.foreach { receiverType =>
      val environment =
        TypeEnvironment.empty.declare("value", TypeBinding(receiverType, false))
      val error = intercept[TypeError](check("value[0]", environment))

      error.pos shouldBe spnuts.ast.SourcePos("<test>", 1, 1)
      error.actual shouldBe Some(receiverType)
    }
  }

  it should "reject known non-sliceable receivers at the receiver position" in {
    val receiverTypes = List(
      UnitType,
      BooleanType,
      LongType,
      DoubleType,
      CharType,
      NullType,
      FunctionType(Nil, AnyType)
    )
    val pos = spnuts.ast.SourcePos("<test>", 1, 1)

    receiverTypes.foreach { receiverType =>
      val receiver = spnuts.ast.Ident("value", pos)
      val expression = spnuts.ast.RangeAccess(
        receiver,
        spnuts.ast.IntLit(0, "0", pos),
        None,
        pos
      )
      val environment =
        TypeEnvironment.empty.declare("value", TypeBinding(receiverType, false))
      val error =
        intercept[TypeError](TypeChecker.check(expression, environment))

      error.pos shouldBe receiver.pos
      error.actual shouldBe Some(receiverType)
    }
  }

  it should "keep dynamic index and range receivers conservative" in {
    val receiverTypes = List(AnyType, NamedType("Dynamic"))
    val pos = spnuts.ast.SourcePos("<test>", 1, 1)

    receiverTypes.foreach { receiverType =>
      val environment =
        TypeEnvironment.empty.declare("value", TypeBinding(receiverType, false))
      check("value[0]", environment).resultType shouldBe AnyType

      val expression = spnuts.ast.RangeAccess(
        spnuts.ast.Ident("value", pos),
        spnuts.ast.IntLit(0, "0", pos),
        None,
        pos
      )
      TypeChecker.check(expression, environment).resultType shouldBe AnyType
    }
  }

  it should "bind collection components inside foreach scopes" in {
    val pos = spnuts.ast.SourcePos("<test>", 1, 1)
    val keyRef = spnuts.ast.Ident("key", pos)
    val valueRef = spnuts.ast.Ident("value", pos)
    val body = spnuts.ast.ExprList(List(keyRef, valueRef), pos)
    val iterable = spnuts.ast.MapExpr(
      List(spnuts.ast.StringLit("x", pos) -> spnuts.ast.IntLit(1, "1", pos)),
      pos
    )
    val expression = spnuts.ast.ForEachExpr(List("key", "value"), iterable, body, pos)
    val result = TypeChecker.check(expression, TypeEnvironment.empty)

    result.table.get(keyRef) shouldBe Some(StringType)
    result.table.get(valueRef) shouldBe Some(LongType)
    result.nextEnvironment.lookup("key") shouldBe None
    result.nextEnvironment.lookup("value") shouldBe None
  }

  it should "traverse loop conditions and restore catch scopes" in {
    intercept[TypeError](check("while (1) 0"))
    intercept[TypeError](check("do 0 while (1)"))
    intercept[TypeError](check("for (; 1; ) 0"))

    val pos = spnuts.ast.SourcePos("<test>", 1, 1)
    val caughtRef = spnuts.ast.Ident("problem", pos)
    val expression = spnuts.ast.TryExpr(
      spnuts.ast.IntLit(1, "1", pos),
      List(
        spnuts.ast.CatchClause(
          "problem",
          Some(spnuts.ast.TypeExpr(List("String"), Nil)),
          caughtRef,
          pos
        )
      ),
      None,
      pos
    )
    val result = TypeChecker.check(expression, TypeEnvironment.empty)

    result.table.get(caughtRef) shouldBe Some(StringType)
    result.nextEnvironment.lookup("problem") shouldBe None
  }

  it should "traverse exception and control-flow payloads" in {
    intercept[TypeError](check("""try { 1 } finally { true - "x" }"""))
    intercept[TypeError](check("""throw true - "x""""))
    intercept[TypeError](check("""return true - "x""""))
    intercept[TypeError](check("""yield true - "x""""))
    intercept[TypeError](check("""break true - "x""""))
  }
