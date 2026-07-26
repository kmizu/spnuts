package spnuts.typing

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spnuts.ast.*
import spnuts.parser.Parser
import spnuts.typing.StaticType.*

class TypeCheckerSpec extends AnyFlatSpec with Matchers:
  private def check(code: String, env: TypeEnvironment = TypeEnvironment.empty) =
    TypeChecker.check(Parser.parse(code, "<test>"), env)

  private def invalidExpression(pos: SourcePos): Expr =
    BinaryExpr(
      BinOp.Sub,
      BoolLit(true, pos),
      StringLit("x", pos),
      pos
    )

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

    val arrayRef = spnuts.ast.Ident("values", pos)
    val slice = spnuts.ast.RangeAccess(
      arrayRef,
      spnuts.ast.IntLit(0, "0", pos),
      None,
      pos
    )
    val environment =
      TypeEnvironment.empty.declare("values", TypeBinding(ArrayType(LongType), false))
    TypeChecker.check(slice, environment).resultType shouldBe ArrayType(LongType)
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

  it should "reject collection receivers unsupported by runtime range access" in {
    val receiverTypes = List(
      ListType(LongType),
      MapType(StringType, LongType)
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
    val receiverTypes = List(AnyType, NamedType("Dynamic"), TypeVariable("T"))
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

  it should "use Any for unannotated parameters and infer returns" in {
    val result = check("function id(x) x")
    result.nextEnvironment.lookup("id").map(_.tpe) shouldBe
      Some(FunctionType(List(AnyType), AnyType, None))
  }

  it should "check annotated parameters and returns before execution" in {
    check("function add(x: Long, y: Long): Long x + y").resultType shouldBe
      FunctionType(List(LongType, LongType), LongType, None)
    val error = intercept[TypeError] {
      check("""function bad(x: Long): String x + 1""")
    }
    error.expected shouldBe Some(StringType)
    error.actual shouldBe Some(LongType)
  }

  it should "predeclare named functions for recursion" in {
    noException should be thrownBy check(
      "function fact(n: Long): Long if (n <= 1) 1 else n * fact(n - 1)"
    )
  }

  it should "validate known function arity and arguments" in {
    intercept[TypeError] {
      check("""function f(x: Long): Long x; f("bad")""")
    }
    intercept[TypeError] {
      check("function f(x: Long): Long x; f()")
    }
  }

  it should "validate known vararg arguments" in {
    check("function f(xs: Long*): Long 0; f(1, 2, 3)").resultType shouldBe LongType

    val error = intercept[TypeError] {
      check("""function f(xs: Long*): Long 0; f(1, "bad")""")
    }
    error.expected shouldBe Some(LongType)
    error.actual shouldBe Some(StringType)
  }

  it should "collect explicit returns within the innermost function" in {
    check(
      """function outer(): Long {
        |  function inner(): String { return "inner" }
        |  return 1
        |}""".stripMargin
    ).resultType shouldBe FunctionType(Nil, LongType, None)

    val error = intercept[TypeError] {
      check("""function bad(x: Long): Long { if (true) return "bad"; x }""")
    }
    error.expected shouldBe Some(LongType)
    error.actual shouldBe Some(StringType)
  }

  it should "keep class body returns out of enclosing functions" in {
    val pos = SourcePos("<test>", 1, 1)
    val method = MethodDef(
      Some(List("String")),
      "text",
      Nil,
      ReturnExpr(Some(StringLit("method", pos)), pos)
    )
    val field = FieldDef(
      Some(List("String")),
      "label",
      Some(ReturnExpr(Some(StringLit("field", pos)), pos))
    )
    val outer = FuncDef(
      Some("outer"),
      Nil,
      false,
      Block(
        List(
          ClassDef(
            "Inner",
            None,
            Nil,
            ClassDefBody(List(field), List(method)),
            pos
          ),
          IntLit(1, "1", pos)
        ),
        pos
      ),
      pos,
      returnType = Some(TypeExpr(List("Long")))
    )

    TypeChecker.check(outer, TypeEnvironment.empty).resultType shouldBe
      FunctionType(Nil, LongType, None)
  }

  it should "normalize function type variables" in {
    check("function id<T>(x: T): T x").resultType shouldBe
      FunctionType(List(TypeVariable("T")), TypeVariable("T"), None)
  }

  it should "reject known non-function values" in {
    val error = intercept[TypeError] {
      check("val value = 1; value()")
    }
    error.actual shouldBe Some(LongType)
  }

  it should "keep overloaded function groups dynamic" in {
    val result = check(
      """function f(x: Long): Long x
        |function f(x: Long, y: Long): Long x + y
        |f(1)""".stripMargin
    )

    result.resultType shouldBe AnyType
    result.nextEnvironment.lookup("f") shouldBe Some(TypeBinding(AnyType, false))
  }

  it should "treat Java, host, and eval results as Any" in {
    check("java.lang.System.currentTimeMillis()").resultType shouldBe AnyType
    check("""eval("1")""").resultType shouldBe AnyType
    check("unknownHostBinding").resultType shouldBe AnyType
  }

  it should "traverse receivers and arguments at dynamic member boundaries" in {
    val pos = SourcePos("<test>", 1, 1)
    val invalid = invalidExpression(pos)
    val safeReceiver = Ident("dynamic", pos)
    val expressions = List[Expr](
      InstanceofExpr(invalid, List("String"), pos),
      MemberAccess(invalid, "value", pos),
      StaticMemberAccess(invalid, "value", pos),
      MethodCall(invalid, "call", Nil, pos),
      MethodCall(safeReceiver, "call", List(invalid), pos),
      StaticMethodCall(invalid, "call", Nil, pos),
      StaticMethodCall(safeReceiver, "call", List(invalid), pos)
    )

    expressions.foreach { expression =>
      intercept[TypeError] {
        TypeChecker.check(expression, TypeEnvironment.empty)
      }
    }
  }

  it should "traverse every nested construction and class expression" in {
    val pos = SourcePos("<test>", 1, 1)
    val invalid = invalidExpression(pos)
    val safe = IntLit(1, "1", pos)
    val invalidField = FieldDef(None, "field", Some(invalid))
    val invalidMethod = MethodDef(None, "method", Nil, invalid)
    val expressions = List[Expr](
      NewExpr(List("Widget"), List(invalid), Nil, None, pos),
      NewExpr(List("Widget"), Nil, List(invalid), None, pos),
      NewExpr(
        List("Widget"),
        Nil,
        Nil,
        Some(ClassDefBody(List(invalidField), Nil)),
        pos
      ),
      NewExpr(
        List("Widget"),
        Nil,
        Nil,
        Some(ClassDefBody(Nil, List(invalidMethod))),
        pos
      ),
      CastExpr(List("Widget"), 0, invalid, pos),
      ClassExpr(invalid, pos),
      BeanDef(List("Widget"), List(BeanProperty("value", invalid, false)), pos),
      ClassDef(
        "Widget",
        None,
        Nil,
        ClassDefBody(List(invalidField), Nil),
        pos
      ),
      ClassDef(
        "Widget",
        None,
        Nil,
        ClassDefBody(Nil, List(invalidMethod)),
        pos
      )
    )

    expressions.foreach { expression =>
      intercept[TypeError] {
        TypeChecker.check(expression, TypeEnvironment.empty)
      }
    }

    noException should be thrownBy TypeChecker.check(
      NewExpr(List("Widget"), List(safe), List(safe), None, pos),
      TypeEnvironment.empty
    )
  }

  it should "traverse dynamic package and import names" in {
    val pos = SourcePos("<test>", 1, 1)
    val invalid = invalidExpression(pos)

    intercept[TypeError] {
      TypeChecker.check(
        PackageExpr(List("example"), Some(invalid), pos),
        TypeEnvironment.empty
      )
    }
    intercept[TypeError] {
      TypeChecker.check(
        ImportExpr(List("example"), false, false, Some(invalid), pos),
        TypeEnvironment.empty
      )
    }
  }

  it should "assign deliberate conservative types to dynamic AST nodes" in {
    val pos = SourcePos("<test>", 1, 1)
    val safe = IntLit(1, "1", pos)

    TypeChecker.check(
      InstanceofExpr(safe, List("String"), pos),
      TypeEnvironment.empty
    ).resultType shouldBe BooleanType
    TypeChecker.check(
      MemberAccess(safe, "value", pos),
      TypeEnvironment.empty
    ).resultType shouldBe AnyType
    TypeChecker.check(
      MethodCall(safe, "call", Nil, pos),
      TypeEnvironment.empty
    ).resultType shouldBe AnyType
    TypeChecker.check(
      NewExpr(List("example", "Widget"), Nil, Nil, None, pos),
      TypeEnvironment.empty
    ).resultType shouldBe NamedType("example.Widget")
    TypeChecker.check(
      CastExpr(List("example", "Widget"), 0, safe, pos),
      TypeEnvironment.empty
    ).resultType shouldBe NamedType("example.Widget")
    TypeChecker.check(
      ClassExpr(safe, pos),
      TypeEnvironment.empty
    ).resultType shouldBe AnyType
    TypeChecker.check(
      ClassRef(List("example", "Widget"), pos),
      TypeEnvironment.empty
    ).resultType shouldBe NamedType("example.Widget")
    TypeChecker.check(
      BeanDef(List("example", "Widget"), Nil, pos),
      TypeEnvironment.empty
    ).resultType shouldBe NamedType("example.Widget")
    TypeChecker.check(
      ClassDef("Widget", None, Nil, ClassDefBody(Nil, Nil), pos),
      TypeEnvironment.empty
    ).resultType shouldBe UnitType
    TypeChecker.check(
      PackageExpr(List("example"), None, pos),
      TypeEnvironment.empty
    ).resultType shouldBe UnitType
    TypeChecker.check(
      ImportExpr(List("example"), false, false, None, pos),
      TypeEnvironment.empty
    ).resultType shouldBe UnitType

    val recordResult = TypeChecker.check(
      RecordDef("Person", List(RecordField(Some("String"), "name")), pos),
      TypeEnvironment.empty
    )
    recordResult.resultType shouldBe UnitType
    recordResult.nextEnvironment.lookup("Person") shouldBe
      Some(TypeBinding(AnyType, false))
  }
