package spnuts.interpreter

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spnuts.parser.Parser
import spnuts.runtime.Context

class InterpreterSpec extends AnyFlatSpec with Matchers:

  def run(src: String): Any =
    val ast = Parser.parse(src, "<test>")
    val ctx = Context()
    Interpreter.eval(ast, ctx)

  /** Context with built-in functions (pnuts.lib) pre-installed. */
  def runLib(src: String): Any =
    val ast = Parser.parse(src, "<test>")
    val ctx = Context()
    spnuts.runtime.BuiltinModule.install(spnuts.runtime.PnutsPackage.global)
    Interpreter.eval(ast, ctx)

  def runWith(ctx: Context)(src: String): Any =
    Interpreter.eval(Parser.parse(src, "<test>"), ctx)

  "Interpreter" should "evaluate integer arithmetic" in {
    run("2 + 3")     shouldBe 5L
    run("10 - 4")    shouldBe 6L
    run("3 * 4")     shouldBe 12L
    run("10 / 3")    shouldBe 3L
    run("10 % 3")    shouldBe 1L
  }

  it should "evaluate nested arithmetic with correct precedence" in {
    run("2 + 3 * 4") shouldBe 14L  // 2 + 12
    run("(2 + 3) * 4") shouldBe 20L
  }

  it should "evaluate string concatenation" in {
    run(""""hello" + ", " + "world"""") shouldBe "hello, world"
    run(""""x = " + 42""") shouldBe "x = 42"
  }

  it should "evaluate boolean operations" in {
    run("true && false")  shouldBe false
    run("true || false")  shouldBe true
    run("!true")          shouldBe false
  }

  it should "evaluate comparison operators" in {
    run("1 < 2")  shouldBe true
    run("2 > 1")  shouldBe true
    run("1 <= 1") shouldBe true
    run("1 >= 2") shouldBe false
    run("1 == 1") shouldBe true
    run("1 != 2") shouldBe true
  }

  it should "evaluate variable assignment and lookup" in {
    run("x = 10; x") shouldBe 10L
    run("x = 5; y = 3; x + y") shouldBe 8L
  }

  it should "evaluate if-else" in {
    run("if (1 > 0) 42 else -1")  shouldBe 42L
    run("if (0 > 1) 42 else -1")  shouldBe -1L
  }

  it should "evaluate while loop" in {
    run("x = 0; while (x < 5) x = x + 1; x") shouldBe 5L
  }

  it should "evaluate for-each loop" in {
    run("sum = 0; for (x : [1, 2, 3, 4, 5]) sum = sum + x; sum") shouldBe 15L
  }

  it should "evaluate function definition and call" in {
    run("function f(x) x * 2; f(21)") shouldBe 42L
  }

  it should "evaluate recursive functions" in {
    run("""
      function fib(n)
        if (n <= 1) n
        else fib(n - 1) + fib(n - 2)
      fib(10)
    """) shouldBe 55L
  }

  it should "evaluate closures" in {
    run("f = { x -> x * 2 }; f(21)") shouldBe 42L
  }

  it should "capture variables in closures (lexical scope)" in {
    run("""
      x = 10
      f = { y -> x + y }
      f(5)
    """) shouldBe 15L
  }

  it should "evaluate return statement" in {
    run("""
      function abs(x)
        if (x < 0) return -x
        x
      abs(-5)
    """) shouldBe 5L
  }

  it should "evaluate list literal" in {
    val r = run("[1, 2, 3]")
    r.isInstanceOf[Array[?]] shouldBe true
    r.asInstanceOf[Array[?]].length shouldBe 3
  }

  it should "evaluate index access" in {
    run("a = [10, 20, 30]; a[1]") shouldBe 20L
  }

  it should "evaluate null literal" in {
    run("null") shouldBe (null: Any)
  }

  it should "evaluate unary operators" in {
    run("-5")    shouldBe -5L
    run("~0")    shouldBe -1L
    run("!false") shouldBe true
  }

  it should "evaluate bitwise operators" in {
    run("6 & 3") shouldBe 2L
    run("6 | 3") shouldBe 7L
    run("6 ^ 3") shouldBe 5L
  }

  it should "evaluate shift operators" in {
    run("1 << 3") shouldBe 8L
    run("8 >> 2") shouldBe 2L
  }

  it should "evaluate ternary" in {
    run("1 > 0 ? \"yes\" : \"no\"") shouldBe "yes"
  }

  it should "evaluate compound assignment" in {
    run("x = 5; x += 3; x") shouldBe 8L
    run("x = 10; x -= 4; x") shouldBe 6L
    run("x = 3; x *= 4; x") shouldBe 12L
  }

  it should "evaluate multi-statement blocks" in {
    run("x = 1; y = 2; z = 3; x + y + z") shouldBe 6L
  }

  it should "support shadowing in nested scopes" in {
    run("""
      x = 1
      function inner() {
        x = 99
        x
      }
      inner()
    """) shouldBe 99L
  }

  it should "evaluate break in while loop" in {
    run("""
      x = 0
      while (true) {
        x = x + 1
        if (x >= 3) break
      }
      x
    """) shouldBe 3L
  }

  it should "evaluate string interpolation" in {
    run("""name = "World"; "Hello \(name)!"""") shouldBe "Hello World!"
    run("""x = 42; "x = \(x)"""") shouldBe "x = 42"
    run("""a = 1; b = 2; "\(a) + \(b) = \(a + b)"""") shouldBe "1 + 2 = 3"
  }

  it should "evaluate map literal" in {
    val r = run("""{ "a" => 1, "b" => 2 }""")
    r.isInstanceOf[java.util.Map[?, ?]] shouldBe true
    r.asInstanceOf[java.util.Map[?, ?]].get("a") shouldBe 1L
  }

  it should "evaluate do-while loop" in {
    run("""
      x = 0
      do { x = x + 1 } while (x < 3)
      x
    """) shouldBe 3L
  }

  it should "evaluate switch statement" in {
    // switch has C-style fallthrough; use break to exit, side-effect for result
    run("""
      x = 2
      r = "none"
      switch (x) {
        case 1: r = "one"; break
        case 2: r = "two"; break
        default: r = "other"
      }
      r
    """) shouldBe "two"
  }

  it should "evaluate try-catch" in {
    // throw a string — interpreter wraps it in RuntimeException
    run("""
      try {
        throw "oops"
      } catch (java.lang.RuntimeException e) {
        "caught"
      }
    """) shouldBe "caught"
  }

  it should "evaluate for C-style loop" in {
    run("""
      sum = 0
      for (i = 0; i < 5; i = i + 1) sum = sum + i
      sum
    """) shouldBe 10L
  }

  // ── Built-in functions (pnuts.lib) ─────────────────────────────────────────

  it should "call size() on array and string" in {
    runLib("size([1, 2, 3])") shouldBe 3L
    runLib("""size("hello")""") shouldBe 5L
  }

  it should "call range()" in {
    val r = runLib("range(0, 5)")
    r.asInstanceOf[Array[Any]].toList shouldBe List(0L, 1L, 2L, 3L, 4L)
  }

  it should "call filter() with lambda" in {
    val r = runLib("filter([1, 2, 3, 4, 5], { n -> n > 2 })")
    import scala.jdk.CollectionConverters.*
    r.asInstanceOf[java.util.List[Any]].asScala.toList shouldBe List(3L, 4L, 5L)
  }

  it should "call map() with lambda" in {
    val r = runLib("map([1, 2, 3], { n -> n * 2 })")
    import scala.jdk.CollectionConverters.*
    r.asInstanceOf[java.util.List[Any]].asScala.toList shouldBe List(2L, 4L, 6L)
  }

  it should "call reduce()" in {
    runLib("reduce([1, 2, 3, 4], { acc, n -> acc + n }, 0)") shouldBe 10L
  }

  it should "call str() and int()" in {
    runLib("str(42)") shouldBe "42"
    runLib("""int("123")""") shouldBe 123L
  }

  it should "call join()" in {
    runLib("""join(["a", "b", "c"], ", ")""") shouldBe "a, b, c"
  }

  it should "call abs() and max() and min()" in {
    runLib("abs(-5)") shouldBe 5L
    runLib("max(3, 7)") shouldBe 7L
    runLib("min(3, 7)") shouldBe 3L
  }

  it should "not confuse function arg (ident, string) with multi-assign" in {
    // regression: join(xs, ", ") was misread as multi-assign xs, "..."
    runLib("""xs = [1, 2, 3]; join(xs, "-")""") shouldBe "1-2-3"
  }

  it should "evaluate multi-assign" in {
    run("a, b = [10, 20]; a + b") shouldBe 30L
  }

  // ── Range expressions ────────────────────────────────────────────────────────

  it should "iterate for-each over range" in {
    run("sum = 0; for (x : 1..5) sum = sum + x; sum") shouldBe 15L
  }

  it should "iterate for-each over range starting at 0" in {
    run("sum = 0; for (x : 0..4) sum = sum + x; sum") shouldBe 10L
  }

  it should "evaluate range expression in for-each" in {
    run("s = 0; for (x : 1..5) s = s + x; s") shouldBe 15L
  }

  // ── Generator (yield) ────────────────────────────────────────────────────────

  it should "evaluate generator with yield" in {
    val r = run("""
      function gen(n) {
        i = 0
        while (i < n) {
          yield i
          i = i + 1
        }
      }
      gen(3)
    """)
    r.asInstanceOf[Array[Any]].toList shouldBe List(0L, 1L, 2L)
  }

  // ── Built-in: eval ───────────────────────────────────────────────────────────

  it should "call eval() to execute code strings" in {
    runLib("""eval("1 + 2")""") shouldBe 3L
    runLib("""eval("\"hello\"")""") shouldBe "hello"
  }

  // ── Built-in: type ───────────────────────────────────────────────────────────

  it should "call type() to get type name" in {
    runLib("type(42)") shouldBe "Long"
    runLib("""type("hi")""") shouldBe "String"
    runLib("type(null)") shouldBe "null"
  }

  // ── Built-in: math extras ────────────────────────────────────────────────────

  it should "call sqrt and pow" in {
    runLib("sqrt(4.0)") shouldBe 2.0
    runLib("pow(2.0, 10.0)") shouldBe 1024.0
  }

  // ── Built-in: string extras ──────────────────────────────────────────────────

  it should "call concat and charAt" in {
    runLib("""concat("hello", " ", "world")""") shouldBe "hello world"
    runLib("""charAt("abc", 1)""") shouldBe 'b'
  }

  // ── Built-in: collection extras ──────────────────────────────────────────────

  it should "call first and last" in {
    runLib("first([10, 20, 30])") shouldBe 10L
    runLib("last([10, 20, 30])")  shouldBe 30L
  }

  it should "call toList and toArray" in {
    val lst = runLib("toList([1, 2, 3])")
    lst.isInstanceOf[java.util.List[?]] shouldBe true
    val arr = runLib("toArray(toList([1, 2, 3]))")
    arr.isInstanceOf[Array[?]] shouldBe true
  }

  it should "call flatten" in {
    import scala.jdk.CollectionConverters.*
    val r = runLib("flatten([[1, 2], [3, 4]])")
    r.asInstanceOf[java.util.List[Any]].asScala.toList shouldBe List(1L, 2L, 3L, 4L)
  }

  // ── Built-in: matches / replaceAll ───────────────────────────────────────────

  it should "call matches and replaceAll" in {
    runLib("""matches("hello", "h.*o")""") shouldBe true
    runLib("""replaceAll("hello world", "o", "0")""") shouldBe "hell0 w0rld"
  }

  // ── Array slice with RangeAccess ─────────────────────────────────────────────

  it should "slice array with range access" in {
    val r = run("a = [10, 20, 30, 40, 50]; a[1..3]")
    r.asInstanceOf[Array[?]].toList shouldBe List(20L, 30L, 40L)
  }
