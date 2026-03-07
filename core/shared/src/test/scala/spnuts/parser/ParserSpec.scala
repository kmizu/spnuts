package spnuts.parser

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spnuts.ast.*

class ParserSpec extends AnyFlatSpec with Matchers:

  def parse(src: String): Expr =
    val exprs = Parser.parse(src, "<test>").exprs
    if exprs.size == 1 then exprs.head
    else ExprList(exprs, SourcePos("<test>", 1, 1))

  "Parser" should "parse integer literals" in {
    parse("42")  shouldBe an[IntLit]
    parse("0")   shouldBe an[IntLit]
    parse("100L") shouldBe an[IntLit]
    parse("0xFF") shouldBe an[IntLit]
    parse("#FF")  shouldBe an[IntLit]
  }

  it should "parse float literals" in {
    parse("3.14")  shouldBe an[FloatLit]
    parse("1e10")  shouldBe an[FloatLit]
  }

  it should "parse string literals" in {
    parse("\"hello\"") shouldBe StringLit("hello", SourcePos("<test>", 1, 1))
  }

  it should "parse boolean literals" in {
    parse("true")  shouldBe an[BoolLit]
    parse("false") shouldBe an[BoolLit]
  }

  it should "parse null" in {
    parse("null") shouldBe a[NullLit]
  }

  it should "parse arithmetic expressions" in {
    parse("1 + 2") should matchPattern { case BinaryExpr(BinOp.Add, IntLit(1L, "1", _), IntLit(2L, "2", _), _) => }
    parse("3 * 4 + 5") shouldBe an[BinaryExpr]
  }

  it should "respect operator precedence" in {
    // 2 + 3 * 4 should be 2 + (3 * 4)
    parse("2 + 3 * 4") match
      case BinaryExpr(BinOp.Add, IntLit(2L, _, _), BinaryExpr(BinOp.Mul, IntLit(3L, _, _), IntLit(4L, _, _), _), _) =>
        succeed
      case other => fail(s"Unexpected: $other")
  }

  it should "parse parenthesized expressions" in {
    parse("(2 + 3) * 4") match
      case BinaryExpr(BinOp.Mul, BinaryExpr(BinOp.Add, _, _, _), _, _) => succeed
      case other => fail(s"Unexpected: $other")
  }

  it should "parse identifier" in {
    parse("x") should matchPattern { case Ident("x", _) => }
  }

  it should "parse assignment" in {
    parse("x = 42") shouldBe an[Assignment]
  }

  it should "parse if expression" in {
    parse("if (x > 0) x else -x") shouldBe an[IfExpr]
  }

  it should "parse while loop" in {
    parse("while (i < 10) { i = i + 1 }") shouldBe an[WhileExpr]
  }

  it should "parse function definition" in {
    parse("function f(x) x * 2") shouldBe an[FuncDef]
  }

  it should "parse function call" in {
    parse("f(1, 2, 3)") shouldBe an[FuncCall]
  }

  it should "parse method call" in {
    parse("obj.toString()") shouldBe an[MethodCall]
  }

  it should "parse member access" in {
    parse("obj.length") shouldBe an[MemberAccess]
  }

  it should "parse index access" in {
    parse("arr[0]") shouldBe an[IndexAccess]
  }

  it should "parse range access" in {
    parse("arr[1..3]") shouldBe an[RangeAccess]
  }

  it should "parse list literal" in {
    parse("[1, 2, 3]") shouldBe an[ListExpr]
  }

  it should "parse map literal" in {
    parse("""{ "a" => 1, "b" => 2 }""") shouldBe an[MapExpr]
  }

  it should "parse ternary" in {
    parse("x > 0 ? x : -x") shouldBe an[TernaryExpr]
  }

  it should "parse for-each loop" in {
    parse("for (x : items) { print(x) }") shouldBe an[ForEachExpr]
  }

  it should "parse closure" in {
    parse("{ x, y -> x + y }") shouldBe an[FuncDef]
  }

  it should "parse return" in {
    parse("return 42") shouldBe an[ReturnExpr]
  }

  it should "parse break" in {
    parse("break") shouldBe an[BreakExpr]
  }

  it should "parse global reference" in {
    parse("::x") should matchPattern { case GlobalRef("x", _) => }
  }

  it should "parse multiple statements" in {
    val ast = Parser.parse("x = 1; y = 2; x + y", "<test>")
    ast.exprs should have size 3
  }

  it should "handle newlines as separators" in {
    val ast = Parser.parse("x = 1\ny = 2\nx + y", "<test>")
    ast.exprs should have size 3
  }
