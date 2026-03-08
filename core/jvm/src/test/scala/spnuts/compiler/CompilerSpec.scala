package spnuts.compiler

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import spnuts.parser.Parser
import spnuts.runtime.{Context, BuiltinModule, JvmPlatform, PnutsPackage}
import spnuts.interpreter.Interpreter

/** Tests that the JIT compiler produces results identical to the interpreter. */
class CompilerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll:

  override def beforeAll(): Unit =
    JvmPlatform.init()
    BuiltinModule.install(PnutsPackage.global)

  def freshCtx(): Context =
    val pkg = PnutsPackage("compilerTest", Some(PnutsPackage.global))
    Context(currentPackage = pkg)

  def interpreted(src: String): Any =
    val ast = Parser.parse(src, "<test>")
    Interpreter.eval(ast, freshCtx())

  def compiled(src: String): Any =
    val ast = Parser.parse(src, "<test>")
    val pkg = PnutsPackage.global
    ast match
      case el: spnuts.ast.ExprList =>
        Compiler.compileScript(el, pkg) match
          case Some(fn) => fn(freshCtx())
          case None => fail("Compiler returned None for: " + src)
      case _ => fail("Expected ExprList")

  def bothEqual(src: String): Unit =
    val i = interpreted(src)
    val c = compiled(src)
    withClue(s"interpreted=$i compiled=$c for: $src") {
      (i, c) match
        case (a: Number, b: Number) => a.doubleValue() shouldBe b.doubleValue()
        case _ => String.valueOf(i) shouldBe String.valueOf(c)
    }

  // ── Literals ────────────────────────────────────────────────────────────────

  "Compiler" should "compile integer literal" in { bothEqual("42") }

  it should "compile float literal" in { bothEqual("3.14") }

  it should "compile string literal" in { bothEqual("\"hello\"") }

  it should "compile boolean true" in { bothEqual("true") }

  it should "compile boolean false" in { bothEqual("false") }

  it should "compile null literal" in { bothEqual("null") }

  // ── Arithmetic ──────────────────────────────────────────────────────────────

  it should "compile addition" in { bothEqual("1 + 2") }

  it should "compile subtraction" in { bothEqual("10 - 3") }

  it should "compile multiplication" in { bothEqual("6 * 7") }

  it should "compile division" in { bothEqual("10 / 2") }

  it should "compile modulo" in { bothEqual("17 % 5") }

  it should "compile mixed arithmetic" in { bothEqual("2 + 3 * 4") }

  it should "compile parenthesized expression" in { bothEqual("(2 + 3) * 4") }

  // ── Comparison ──────────────────────────────────────────────────────────────

  it should "compile equality" in { bothEqual("1 == 1") }

  it should "compile inequality" in { bothEqual("1 != 2") }

  it should "compile less-than" in { bothEqual("3 < 5") }

  it should "compile greater-than" in { bothEqual("5 > 3") }

  it should "compile less-than-or-equal" in { bothEqual("5 <= 5") }

  it should "compile greater-than-or-equal" in { bothEqual("5 >= 3") }

  // ── Logical ─────────────────────────────────────────────────────────────────

  it should "compile logical and" in { bothEqual("true && false") }

  it should "compile logical or" in { bothEqual("false || true") }

  it should "compile logical not" in { bothEqual("!true") }

  it should "compile short-circuit &&" in { bothEqual("false && true") }

  it should "compile short-circuit ||" in { bothEqual("true || false") }

  // ── Variables ───────────────────────────────────────────────────────────────

  it should "compile assignment and reference" in { bothEqual("x = 42; x") }

  it should "compile sequential assignments" in { bothEqual("x = 1; y = 2; x + y") }

  it should "compile compound assignment +=" in { bothEqual("x = 10; x += 5; x") }

  // ── Control flow ────────────────────────────────────────────────────────────

  it should "compile if-else (true branch)" in { bothEqual("if (true) 1 else 2") }

  it should "compile if-else (false branch)" in { bothEqual("if (false) 1 else 2") }

  it should "compile if without else returning null" in {
    compiled("if (false) 1") shouldBe (null: Any)
  }

  it should "compile ternary expression" in { bothEqual("true ? 42 : 0") }

  it should "compile while loop" in { bothEqual("i = 0; while (i < 5) i = i + 1; i") }

  it should "compile for loop" in {
    bothEqual("sum = 0; for (i = 1; i <= 10; i++) sum = sum + i; sum")
  }

  // ── Strings ─────────────────────────────────────────────────────────────────

  it should "compile string concatenation" in { bothEqual(""""hello" + " world"""") }

  // ── Collections ─────────────────────────────────────────────────────────────

  it should "compile list literal" in {
    val c = compiled("[1, 2, 3]")
    c.asInstanceOf[Array[Any]].map(_.asInstanceOf[Number].longValue()) shouldBe Array(1L, 2L, 3L)
  }

  it should "compile map literal" in {
    val c = compiled("""{"a" => 1, "b" => 2}""")
    val m = c.asInstanceOf[java.util.Map[Any, Any]]
    m.get("a").asInstanceOf[Number].longValue() shouldBe 1L
    m.get("b").asInstanceOf[Number].longValue() shouldBe 2L
  }

  // ── Multi-statement scripts ──────────────────────────────────────────────────

  it should "compile multi-statement script returning last value" in {
    bothEqual("x = 10; y = 20; x + y")
  }

  it should "compile loop accumulator" in {
    bothEqual("sum = 0; i = 1; while (i <= 10) { sum = sum + i; i = i + 1 }; sum")
  }

  it should "compile fibonacci" in {
    bothEqual("a = 0; b = 1; i = 0; while (i < 10) { tmp = a + b; a = b; b = tmp; i++ }; a")
  }

  // ── Function definitions ─────────────────────────────────────────────────────

  it should "compile top-level function definition and call" in {
    bothEqual("""
      function add(x, y) { x + y }
      add(3, 4)
    """)
  }

  it should "compile recursive function" in {
    bothEqual("""
      function fib(n) if (n <= 1) n else fib(n - 1) + fib(n - 2)
      fib(10)
    """)
  }

  it should "compile function using closure over variable" in {
    bothEqual("""
      n = 10
      function addN(x) { x + n }
      addN(5)
    """)
  }

  it should "compile multiple function definitions" in {
    bothEqual("""
      function double(x) { x * 2 }
      function triple(x) { x * 3 }
      double(3) + triple(2)
    """)
  }

  // ── val / var declarations ────────────────────────────────────────────────────

  it should "compile val declaration" in {
    bothEqual("""
      function test() {
        val x = 42
        x
      }
      test()
    """)
  }

  it should "compile var declaration and mutation" in {
    bothEqual("""
      function test() {
        var s = "hello"
        s = "world"
        s
      }
      test()
    """)
  }

  it should "compile val at top level" in {
    bothEqual("val answer = 21 * 2; answer")
  }

  // ── Closures ──────────────────────────────────────────────────────────────────

  it should "compile closure assigned to variable" in {
    bothEqual("""
      double = { x -> x * 2 }
      double(21)
    """)
  }

  it should "compile higher-order function with closure" in {
    bothEqual("""
      function apply(f, x) { f(x) }
      apply({ n -> n + 1 }, 41)
    """)
  }

  it should "compile closure capturing outer variable" in {
    bothEqual("""
      base = 100
      addBase = { x -> x + base }
      addBase(42)
    """)
  }

  it should "compile function returning a closure" in {
    bothEqual("""
      function makeAdder(n) { { x -> x + n } }
      add5 = makeAdder(5)
      add5(37)
    """)
  }

  // ── Break / continue ──────────────────────────────────────────────────────────

  it should "compile while loop with break" in {
    bothEqual("""
      i = 0
      while (true) {
        if (i >= 5) break
        i = i + 1
      }
      i
    """)
  }

  it should "compile for loop with break returning value" in {
    bothEqual("""
      result = 0
      i = 0
      while (i < 100) {
        if (i == 42) { break i }
        i = i + 1
      }
      i
    """)
  }

  it should "compile for-each with continue" in {
    bothEqual("""
      sum = 0
      for (x : [1, 2, 3, 4, 5, 6]) {
        if (x % 2 == 0) continue
        sum = sum + x
      }
      sum
    """)
  }

  it should "compile C-style for loop with break" in {
    bothEqual("""
      result = -1
      for (i = 0; i < 10; i++) {
        if (i == 7) { result = i; break }
      }
      result
    """)
  }

  // ── Switch ────────────────────────────────────────────────────────────────────

  it should "compile switch with break" in {
    bothEqual("""
      x = 2
      switch (x) {
        case 1: "one"; break
        case 2: "two"; break
        case 3: "three"; break
        default: "other"
      }
    """)
  }

  it should "compile switch with default" in {
    bothEqual("""
      x = 99
      switch (x) {
        case 1: "one"; break
        default: "other"
      }
    """)
  }

  it should "compile switch with fallthrough" in {
    bothEqual("""
      x = 1
      result = ""
      switch (x) {
        case 1: result = result + "a"
        case 2: result = result + "b"; break
        case 3: result = result + "c"; break
      }
      result
    """)
  }
