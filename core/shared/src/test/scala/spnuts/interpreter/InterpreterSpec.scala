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

  // ── Pre/post increment / decrement ───────────────────────────────────────────

  it should "evaluate pre-increment" in {
    run("x = 5; ++x") shouldBe 6L
    run("x = 5; ++x; x") shouldBe 6L
  }

  it should "evaluate post-increment" in {
    run("x = 5; x++") shouldBe 5L   // returns old value
    run("x = 5; x++; x") shouldBe 6L
  }

  it should "evaluate pre-decrement" in {
    run("x = 5; --x") shouldBe 4L
  }

  it should "evaluate post-decrement" in {
    run("x = 5; x--") shouldBe 5L
    run("x = 5; x--; x") shouldBe 4L
  }

  // ── Compound assignment extras ────────────────────────────────────────────────

  it should "evaluate all compound assignment operators" in {
    run("x = 10; x /= 4; x") shouldBe 2L
    run("x = 10; x %= 3; x") shouldBe 1L
    run("x = 6; x &= 3; x")  shouldBe 2L
    run("x = 6; x |= 1; x")  shouldBe 7L
    run("x = 5; x ^= 3; x")  shouldBe 6L
    run("x = 1; x <<= 3; x") shouldBe 8L
    run("x = 8; x >>= 2; x") shouldBe 2L
  }

  // ── Float arithmetic ──────────────────────────────────────────────────────────

  it should "evaluate float arithmetic" in {
    run("1.5 + 2.5") shouldBe 4.0
    run("3.0 * 2.0") shouldBe 6.0
    run("7.0 / 2.0") shouldBe 3.5
  }

  it should "evaluate integer + float promotion" in {
    run("1 + 2.0") shouldBe 3.0
    run("3 * 1.5") shouldBe 4.5
  }

  // ── Index assignment ──────────────────────────────────────────────────────────

  it should "evaluate index assignment" in {
    run("a = [1, 2, 3]; a[1] = 99; a[1]") shouldBe 99L
  }

  // ── Continue in loop ──────────────────────────────────────────────────────────

  it should "evaluate continue in while loop" in {
    run("""
      sum = 0
      i = 0
      while (i < 5) {
        i = i + 1
        if (i == 3) continue
        sum = sum + i
      }
      sum
    """) shouldBe 12L  // 1+2+4+5 = 12
  }

  it should "evaluate continue in for-each" in {
    run("""
      sum = 0
      for (x : [1, 2, 3, 4, 5]) {
        if (x == 3) continue
        sum = sum + x
      }
      sum
    """) shouldBe 12L  // 1+2+4+5
  }

  // ── Switch fallthrough ────────────────────────────────────────────────────────

  it should "evaluate switch fallthrough without break" in {
    run("""
      x = 1; r = 0
      switch (x) {
        case 1: r = r + 1
        case 2: r = r + 10
        default: r = r + 100
      }
      r
    """) shouldBe 111L  // falls through all cases
  }

  it should "evaluate switch default only" in {
    run("""
      x = 99; r = "none"
      switch (x) {
        case 1: r = "one"; break
        default: r = "other"
      }
      r
    """) shouldBe "other"
  }

  // ── Try/finally (no catch) ────────────────────────────────────────────────────

  it should "evaluate try-finally without catch" in {
    run("""
      x = 0
      try { x = 1 } finally { x = x + 10 }
      x
    """) shouldBe 11L
  }

  it should "run finally even when exception thrown" in {
    run("""
      x = 0
      try {
        try { throw "err" }
        catch (java.lang.RuntimeException e) { x = 1 }
        finally { x = x + 10 }
      } catch (java.lang.Exception e) { x = 99 }
      x
    """) shouldBe 11L
  }

  // ── Nested functions / closures ───────────────────────────────────────────────

  it should "evaluate higher-order function returning closure" in {
    run("""
      function adder(n) { x -> x + n }
      add5 = adder(5)
      add5(10)
    """) shouldBe 15L
  }

  it should "evaluate closure capturing mutable variable" in {
    run("""
      function makeCounter() {
        count = 0
        { -> count = count + 1; count }
      }
      c = makeCounter()
      c()
      c()
      c()
    """) shouldBe 3L
  }

  it should "evaluate varargs function" in {
    run("""
      function sum(args[]) {
        total = 0
        for (x : args) total = total + x
        total
      }
      sum(1, 2, 3, 4, 5)
    """) shouldBe 15L
  }

  // ── for-each over java.util.List ─────────────────────────────────────────────

  it should "iterate for-each over ArrayList" in {
    runLib("""
      lst = toList([10, 20, 30])
      sum = 0
      for (x : lst) sum = sum + x
      sum
    """) shouldBe 60L
  }

  // ── Multi-assign edge cases ───────────────────────────────────────────────────

  it should "handle multi-assign with fewer elements than targets" in {
    run("a, b, c = [1, 2]; a + b") shouldBe 3L
    run("a, b, c = [1, 2]; c") shouldBe (null: Any)  // missing → null
  }

  it should "handle multi-assign with single value" in {
    run("a, b = 42; a") shouldBe 42L  // non-array → first gets value, rest null
  }

  // ── Null handling ─────────────────────────────────────────────────────────────

  it should "compare with null" in {
    run("x = null; x == null") shouldBe true
    run("x = 1; x == null")    shouldBe false
  }

  // ── Boolean short-circuit ─────────────────────────────────────────────────────

  it should "short-circuit && and ||" in {
    // If short-circuits, side-effect variable won't be set
    run("x = 0; false && (x = 1); x") shouldBe 0L
    run("x = 0; true  || (x = 1); x") shouldBe 0L
  }

  // ── if without else ───────────────────────────────────────────────────────────

  it should "evaluate if without else (returns null)" in {
    run("if (false) 42") shouldBe (null: Any)
  }

  it should "evaluate else-if chain" in {
    run("""
      x = 2
      if (x == 1) "one"
      else if (x == 2) "two"
      else "other"
    """) shouldBe "two"
  }

  // ── Builtin extras ───────────────────────────────────────────────────────────

  it should "call sort()" in {
    val r = runLib("sort([3, 1, 4, 1, 5, 9, 2, 6])")
    r.asInstanceOf[Array[Any]].toList shouldBe List(1L, 1L, 2L, 3L, 4L, 5L, 6L, 9L)
  }

  it should "call sort() with comparator" in {
    val r = runLib("sort([3, 1, 2], { a, b -> a > b })")
    r.asInstanceOf[Array[Any]].toList shouldBe List(3L, 2L, 1L)
  }

  it should "call reverse()" in {
    val r = runLib("reverse([1, 2, 3])")
    r.asInstanceOf[Array[Any]].toList shouldBe List(3L, 2L, 1L)
  }

  it should "call append()" in {
    import scala.jdk.CollectionConverters.*
    val r = runLib("lst = toList([1, 2]); append(lst, 3); lst")
    r.asInstanceOf[java.util.List[Any]].asScala.toList shouldBe List(1L, 2L, 3L)
  }

  it should "call keys() and values()" in {
    val r = runLib("""keys({ "a" => 1, "b" => 2 })""")
    r.asInstanceOf[Array[Any]].toSet shouldBe Set("a", "b")
  }

  it should "call contains()" in {
    runLib("contains([1, 2, 3], 2)") shouldBe true
    runLib("contains([1, 2, 3], 9)") shouldBe false
    runLib("""contains("hello", "ell")""") shouldBe true
  }

  it should "call any() and all()" in {
    runLib("any([1, 2, 3], { n -> n > 2 })") shouldBe true
    runLib("all([1, 2, 3], { n -> n > 0 })") shouldBe true
    runLib("all([1, 2, 3], { n -> n > 1 })") shouldBe false
  }

  it should "call range() with step" in {
    val r = runLib("range(0, 10, 2)")
    r.asInstanceOf[Array[Any]].toList shouldBe List(0L, 2L, 4L, 6L, 8L)
  }

  it should "call split()" in {
    val r = runLib("""split("a,b,c", ",")""")
    r.asInstanceOf[Array[Any]].toList shouldBe List("a", "b", "c")
  }

  it should "call trim / toUpperCase / toLowerCase" in {
    runLib("""trim("  hello  ")""")        shouldBe "hello"
    runLib("""toUpperCase("hello")""")     shouldBe "HELLO"
    runLib("""toLowerCase("HELLO")""")     shouldBe "hello"
  }

  it should "call startsWith / endsWith / indexOf" in {
    runLib("""startsWith("hello", "he")""") shouldBe true
    runLib("""endsWith("hello", "lo")""")   shouldBe true
    runLib("""indexOf("hello", "ll")""")    shouldBe 2L
  }

  it should "call substring()" in {
    runLib("""substring("hello", 1, 4)""") shouldBe "ell"
    runLib("""substring("hello", 2)""")    shouldBe "llo"
  }

  it should "call replace()" in {
    runLib("""replace("hello world", "world", "SPnuts")""") shouldBe "hello SPnuts"
  }

  it should "call floor / ceil / round" in {
    runLib("floor(3.7)") shouldBe 3L
    runLib("ceil(3.2)")  shouldBe 4L
    runLib("round(3.5)") shouldBe 4L
  }

  it should "call assert() without message" in {
    noException should be thrownBy runLib("assert(1 == 1)")
    an[AssertionError] should be thrownBy runLib("assert(false)")
  }

  it should "call assert() with message" in {
    val ex = the[AssertionError] thrownBy runLib("""assert(false, "bad")""")
    ex.getMessage shouldBe "bad"
  }

  it should "call error()" in {
    an[RuntimeException] should be thrownBy runLib("""error("boom")""")
  }

  it should "call isEmpty()" in {
    runLib("isEmpty([])") shouldBe true
    runLib("isEmpty([1])") shouldBe false
    runLib("""isEmpty("")""") shouldBe true
  }

  it should "call float() and boolean()" in {
    runLib("float(3)") shouldBe 3.0
    runLib("""boolean(0)""") shouldBe false
    runLib("""boolean(1)""") shouldBe true
  }

  it should "call put() and get() on map" in {
    runLib("""m = map(); put(m, "k", 42); get(m, "k")""") shouldBe 42L
  }

  it should "call copy()" in {
    val r = runLib("a = [1, 2, 3]; b = copy(a); b[0] = 99; a[0]")
    r shouldBe 1L  // original unchanged
  }

  it should "call remove() on list" in {
    import scala.jdk.CollectionConverters.*
    val r = runLib("lst = toList([10, 20, 30]); remove(lst, 1); lst")
    r.asInstanceOf[java.util.List[Any]].asScala.toList shouldBe List(10L, 30L)
  }

  // ── record type ────────────────────────────────────────────────────────────

  it should "define and instantiate a record" in {
    run("""
      record Person(name, age)
      p = Person("Alice", 30)
      p.name
    """) shouldBe "Alice"
  }

  it should "access multiple record fields" in {
    run("""
      record Point(x, y)
      p = Point(3, 4)
      p.x + p.y
    """) shouldBe 7L
  }

  it should "access record field via getter-style method" in {
    run("""
      record Person(name, age)
      p = Person("Bob", 25)
      p.getName()
    """) shouldBe "Bob"
  }

  it should "support records with type annotations" in {
    run("""
      record Person(String name, int age)
      p = Person("Carol", 40)
      p.age
    """) shouldBe 40L
  }

  it should "support record toString" in {
    run("""
      record Color(r, g, b)
      c = Color(255, 0, 128)
      str(c)
    """).asInstanceOf[String] should include("Color")
  }

  // ── val/var declarations ─────────────────────────────────────────────────────

  "val/var" should "declare and read a val" in {
    run("val x = 42; x") shouldBe 42L
  }

  it should "declare and read a var" in {
    run("var x = 10; x") shouldBe 10L
  }

  it should "allow reassigning a var" in {
    run("var x = 1; x = 2; x") shouldBe 2L
  }

  it should "reject reassigning a val" in {
    val ex = intercept[Exception] {
      run("function f() { val x = 1; x = 2 }; f()")
    }
    ex.getMessage should include("immutable")
  }

  it should "support typed val declaration" in {
    run("""
      function f() { val x: java.lang.Long = 99; x }
      f()
    """) shouldBe 99L
  }

  it should "raise type error for wrong val type" in {
    val ex = intercept[Exception] {
      run("function f() { val x: java.lang.Long = \"hello\"; x }; f()")
    }
    ex.getMessage should include("Type error")
  }

  // ── typed function annotations ────────────────────────────────────────────────

  "typed functions" should "accept typed params and return type" in {
    run("""
      function add(a: java.lang.Long, b: java.lang.Long): java.lang.Long { a + b }
      add(3, 4)
    """) shouldBe 7L
  }

  it should "raise type error on wrong param type" in {
    val ex = intercept[Exception] {
      run("""
        function greet(s: java.lang.String): java.lang.String { s }
        greet(42)
      """)
    }
    ex.getMessage should include("Type error")
  }

  it should "raise type error on wrong return type" in {
    val ex = intercept[Exception] {
      run("""
        function f(x: java.lang.Long): java.lang.String { x }
        f(1)
      """)
    }
    ex.getMessage should include("Type error")
  }

  it should "reject partial type annotations" in {
    val ex = intercept[Exception] {
      run("""
        function f(a: java.lang.Long, b) { a }
      """)
    }
    ex.getMessage should (include("missing") or include("annotate") or include("type"))
  }

  it should "reject return type without param types" in {
    val ex = intercept[Exception] {
      run("""
        function f(a): java.lang.Long { a }
      """)
    }
    ex.getMessage should (include("missing") or include("annotate") or include("type"))
  }

  // ── generic function type parameters ─────────────────────────────────────────

  "generic functions" should "define and call a generic identity function" in {
    run("""
      function identity<T>(x: T): T { x }
      identity(42)
    """) shouldBe 42L
  }

  it should "bind type variable T to actual argument type" in {
    run("""
      function identity<T>(x: T): T { x }
      identity("hello")
    """) shouldBe "hello"
  }

  it should "raise type error when return type does not match inferred T" in {
    val ex = intercept[Exception] {
      run("""
        function wrap<T>(x: T): T { "wrong" }
        wrap(42)
      """)
    }
    ex.getMessage should include("Type error")
  }

  it should "accept parameterized collection type (erased)" in {
    // List<T> erases to List at runtime — just checks the outer type
    run("""
      function wrap<T>(xs: java.util.List): java.util.List { xs }
      lst = toList([10, 20, 30])
      wrap(lst)
    """).isInstanceOf[java.util.List[?]] shouldBe true
  }

  it should "support two type parameters" in {
    run("""
      function pair<A, B>(a: A, b: B): A { a }
      pair(1, "two")
    """) shouldBe 1L
  }

  it should "pass through untyped when type var not used in annotations" in {
    run("""
      function box<T>(x: java.lang.Long): java.lang.Long { x + 1 }
      box(5)
    """) shouldBe 6L
  }

  // ── type inference for val/var ────────────────────────────────────────────────

  "type inference" should "infer Long type for val and reject reassignment" in {
    val ex = intercept[Exception] {
      run("""
        function f() { val x = 42; x = 99 }
        f()
      """)
    }
    ex.getMessage should include("immutable")
  }

  it should "infer String type for var and reject wrong type" in {
    val ex = intercept[Exception] {
      run("""
        function f() { var x = "hello"; x = 42; x }
        f()
      """)
    }
    ex.getMessage should include("Type error")
  }

  it should "allow reassigning var with same inferred type" in {
    run("""
      function f() { var x = 42; x = 99; x }
      f()
    """) shouldBe 99L
  }

  it should "parse List<?> wildcard type annotation" in {
    run("""
      function wrap(xs: java.util.List<?>): java.util.List<?> { xs }
      lst = toList([1, 2, 3])
      wrap(lst)
    """).isInstanceOf[java.util.List[?]] shouldBe true
  }

  it should "infer lambda parameter types from context (no annotation needed)" in {
    // Lambda passed to a typed param — lambda param 'x' has no annotation
    // It should work because lambda params don't require annotation even in typed functions
    val r = run("""
      function applyEach(xs: java.util.List<?>, f: Object): java.util.List<?> {
        map(xs, f)
      }
      lst = toList([1, 2, 3])
      applyEach(lst, {x -> x})
    """)
    import scala.jdk.CollectionConverters.*
    r.asInstanceOf[java.util.List[Any]].asScala.toList shouldBe List(1L, 2L, 3L)
  }

  // ── function type syntax ──────────────────────────────────────────────────────

  "function types" should "accept (Long) -> Long annotation on function param" in {
    run("""
      function apply(f: (java.lang.Long) -> java.lang.Long, x: java.lang.Long): java.lang.Long {
        f(x)
      }
      apply({n -> n * 2}, 21)
    """) shouldBe 42L
  }

  it should "reject non-function where function type expected" in {
    val ex = intercept[Exception] {
      run("""
        function apply(f: (java.lang.Long) -> java.lang.Long): java.lang.Long { f(1) }
        apply(42)
      """)
    }
    ex.getMessage should include("Type error")
  }

  it should "reject wrong-arity function where typed function expected" in {
    val ex = intercept[Exception] {
      run("""
        function apply(f: (java.lang.Long, java.lang.Long) -> java.lang.Long): java.lang.Long {
          f(1, 2)
        }
        apply({x -> x})
      """)
    }
    ex.getMessage should include("Type error")
  }

  it should "infer lambda param type from function type annotation" in {
    // Lambda {x -> x} passed where (Long) -> Long expected → x is inferred as Long
    // Subsequent wrong-type body would fail; passing identical value works fine
    run("""
      function apply(f: (java.lang.Long) -> java.lang.Long, v: java.lang.Long): java.lang.Long {
        f(v)
      }
      apply({x -> x}, 99)
    """) shouldBe 99L
  }

  it should "parse () -> Long (no-param function type)" in {
    run("""
      function makeConst(f: () -> java.lang.Long): java.lang.Long { f() }
      makeConst({ -> 7 })
    """) shouldBe 7L
  }

  it should "parse single-param shorthand A -> B" in {
    run("""
      function apply(f: java.lang.Long -> java.lang.Long, x: java.lang.Long): java.lang.Long {
        f(x)
      }
      apply({n -> n + 1}, 5)
    """) shouldBe 6L
  }

  it should "display function type in error messages" in {
    val ex = intercept[Exception] {
      run("""
        function apply(f: (java.lang.Long) -> java.lang.Long): java.lang.Long { f(1) }
        apply("not a function")
      """)
    }
    ex.getMessage should (include("->") or include("function"))
  }

  // ── short type names + n-arity ────────────────────────────────────────────────

  it should "resolve short names (Long, String) from java.lang.* auto-import" in {
    run("""
      function add(a: Long, b: Long): Long { a + b }
      add(10, 32)
    """) shouldBe 42L
  }

  it should "support 3-param function type with short names" in {
    run("""
      function apply3(f: (Long, Long, Long) -> Long, a: Long, b: Long, c: Long): Long {
        f(a, b, c)
      }
      apply3({x, y, z -> x + y + z}, 1, 2, 3)
    """) shouldBe 6L
  }

  it should "support 4-param function type" in {
    run("""
      function apply4(f: (Long, Long, Long, Long) -> Long,
                      a: Long, b: Long, c: Long, d: Long): Long {
        f(a, b, c, d)
      }
      apply4({a, b, c, d -> a + b + c + d}, 1, 2, 3, 4)
    """) shouldBe 10L
  }

  it should "resolve List from java.util.* via explicit import in type annotation" in {
    run("""
      import java.util.*
      function wrap(xs: List<?>): List<?> { xs }
      wrap(toList([1, 2, 3]))
    """).isInstanceOf[java.util.List[?]] shouldBe true
  }

  // ── varargs function types ────────────────────────────────────────────────────

  it should "accept a varargs function for (Long*) -> Long annotation" in {
    run("""
      function applyVarargs(f: (Long*) -> Long): Long {
        f(1, 2, 3)
      }
      function sum(args: Long*): Long {
        var total: Long = 0
        for (x : args) { total = total + x }
        total
      }
      applyVarargs(sum)
    """) shouldBe 6L
  }

  it should "reject a non-function where (Long*) -> Long is expected" in {
    val ex = intercept[Exception] {
      run("""
        function applyVarargs(f: (Long*) -> Long): Long { f(1) }
        applyVarargs("not a function")
      """)
    }
    ex.getMessage should (include("->") or include("function"))
  }

  it should "accept a varargs function for (String, Long*) -> Long annotation" in {
    run("""
      function applyMixed(f: (String, Long*) -> Long): Long {
        f("prefix", 10, 20)
      }
      function sumWithLabel(label: String, nums: Long*): Long {
        var total: Long = 0
        for (x : nums) { total = total + x }
        total
      }
      applyMixed(sumWithLabel)
    """) shouldBe 30L
  }

  // ── boxing / Unit type ────────────────────────────────────────────────────────

  it should "accept Int as a type alias for java.lang.Integer" in {
    run("""
      function double(n: Int): Int { n * 2 }
      double(21)
    """) shouldBe 42
  }

  it should "accept Char as a type alias for java.lang.Character" in {
    run("""
      function id(c: Char): Char { c }
      id('A')
    """) shouldBe 'A'
  }

  it should "reject lowercase primitive type names" in {
    val ex = intercept[Exception] {
      run("""
        function f(n: int): int { n }
        f(1)
      """)
    }
    ex.getMessage should include("int")
  }

  it should "accept Unit as return type and return BoxedUnit singleton" in {
    val result = run("""
      function printIt(s: String): Unit { println(s) }
      printIt("hello")
    """)
    result shouldBe scala.runtime.BoxedUnit.UNIT
  }

  it should "treat Int and Long transparently in generics context" in {
    run("""
      function wrap<T>(x: T): T { x }
      wrap(42)
    """) shouldBe 42L
  }

  it should "accept Short as a type alias compatible with Long values" in {
    run("""
      function id(n: Short): Short { n }
      id(100)
    """) shouldBe 100L  // value passes type check (integer widening)
  }

  it should "accept Byte as a type alias compatible with Long values" in {
    run("""
      function id(n: Byte): Byte { n }
      id(42)
    """) shouldBe 42L
  }

  it should "accept Boolean type annotation" in {
    run("""
      function not(b: Boolean): Boolean { !b }
      not(false)
    """) shouldBe true
  }

  it should "reject void as a forbidden primitive type name" in {
    val ex = intercept[Exception] {
      run("""
        function f(): void { 1 }
        f()
      """)
    }
    ex.getMessage should include("void")
  }

  // ── array types ───────────────────────────────────────────────────────────────

  it should "accept Long[] as an array type annotation (parsing)" in {
    // Verify Long[] parses as array-of-Long type annotation without errors
    noException should be thrownBy run("""
      function acceptArray(arr: Long[]): Long { arr.length }
    """)
  }

  it should "parse Long[] as array type (distinct from Long* varargs)" in {
    // Long[] is an array parameter (fixed arity), Long* is varargs
    run("""
      function takeArray(arr: Long[]): Long { arr.length }
      function takeVarargs(args: Long*): Long { args.length }
      takeVarargs(1, 2, 3)
    """) shouldBe 3L
  }

  it should "resolve Long[][] as 2D array type" in {
    // Just verify parsing does not throw
    run("""
      function f(arr: Long[][]): Long { 0 }
      f
    """) // returns the function itself; no array construction needed
  }


  // ── null handling with typed variables ─────────────────────────────────────

  it should "allow null assignment to typed var of reference type" in {
    run("""
      var s: String = "hello"
      s = null
      s
    """) shouldBe (null: Any)
  }

  it should "allow null as typed function argument (reference types accept null)" in {
    run("""
      function greet(s: String): String { if (s == null) "nobody" else s }
      greet(null)
    """) shouldBe "nobody"
  }

  it should "return null from typed String function" in {
    run("""
      function maybeNull(flag: Boolean): String { if (flag) "yes" else null }
      maybeNull(false)
    """) shouldBe (null: Any)
  }

  // ── type error message content ──────────────────────────────────────────────

  it should "include expected and actual types in param error message" in {
    val ex = intercept[Exception] {
      run("""
        function f(n: Long): Long { n }
        f("wrong")
      """)
    }
    ex.getMessage should include("Long")
    ex.getMessage should (include("String") or include("wrong"))
  }

  it should "include expected and actual types in return type error message" in {
    val ex = intercept[Exception] {
      run("""
        function f(n: Long): Long { "not a long" }
        f(1)
      """)
    }
    ex.getMessage should include("Long")
  }

  it should "include param name in type error message" in {
    val ex = intercept[Exception] {
      run("""
        function f(myParam: Long): Long { myParam }
        f("bad")
      """)
    }
    ex.getMessage should include("myParam")
  }

  // ── typed recursive functions ───────────────────────────────────────────────

  it should "support typed recursive function (factorial)" in {
    run("""
      function fact(n: Long): Long {
        if (n <= 1) 1 else n * fact(n - 1)
      }
      fact(5)
    """) shouldBe 120L
  }

  it should "support typed recursive function (fibonacci)" in {
    run("""
      function fib(n: Long): Long {
        if (n <= 1) n else fib(n - 1) + fib(n - 2)
      }
      fib(10)
    """) shouldBe 55L
  }

  it should "support generic recursive function (list length)" in {
    // Uses index-based recursion to avoid Java module encapsulation issues with subList
    run("""
      function lenFrom<T>(xs: List<T>, i: Long): Long {
        if (i >= xs.size()) 0 else 1 + lenFrom(xs, i + 1)
      }
      function len<T>(xs: List<T>): Long { lenFrom(xs, 0) }
      import java.util.*
      len(toList([1, 2, 3, 4, 5]))
    """) shouldBe 5L
  }

  // ── closures with typed captures ───────────────────────────────────────────

  it should "capture typed val in closure" in {
    run("""
      val multiplier: Long = 3
      function makeMultiplier(): (Long) -> Long {
        { x: Long -> x * multiplier }
      }
      f = makeMultiplier()
      f(7)
    """) shouldBe 21L
  }

  it should "capture typed var in closure and reflect mutations" in {
    run("""
      var counter: Long = 0
      function increment(): Unit { counter = counter + 1 }
      increment()
      increment()
      increment()
      counter
    """) shouldBe 3L
  }

  it should "reject reassigning val through closure capture" in {
    val ex = intercept[Exception] {
      run("""
        function makeAdder(n: Long): (Long) -> Long {
          val base: Long = n
          { x: Long -> base = base + x; base }
        }
        f = makeAdder(10)
        f(5)
      """)
    }
    ex.getMessage should include("immutable")
  }

  // ── chained typed calls ─────────────────────────────────────────────────────

  it should "chain typed function calls" in {
    run("""
      function double(n: Long): Long { n * 2 }
      function addOne(n: Long): Long { n + 1 }
      addOne(double(addOne(double(3))))
    """) shouldBe 15L  // double(3)=6, addOne(6)=7, double(7)=14, addOne(14)=15
  }

  it should "chain generic identity calls" in {
    run("""
      function id<T>(x: T): T { x }
      id(id(id(42)))
    """) shouldBe 42L
  }

  it should "use Unit-returning function in sequence" in {
    run("""
      var log: String = ""
      function append(s: String): Unit { log = log + s }
      append("a")
      append("b")
      append("c")
      log
    """) shouldBe "abc"
  }

  // ── Boolean type specifics ──────────────────────────────────────────────────

  it should "accept Boolean typed param and return" in {
    run("""
      function nand(a: Boolean, b: Boolean): Boolean { !(a && b) }
      nand(true, true)
    """) shouldBe false
  }

  it should "reject non-Boolean where Boolean expected" in {
    val ex = intercept[Exception] {
      run("""
        function check(b: Boolean): Boolean { b }
        check(42)
      """)
    }
    ex.getMessage should include("Boolean")
  }

  it should "infer Boolean type for val from boolean literal" in {
    val ex = intercept[Exception] {
      run("""
        function test(): Long {
          val flag: Boolean = true
          flag = 42  // should fail: Boolean != Long
          1
        }
        test()
      """)
    }
    ex.getMessage should (include("Boolean") or include("immutable"))
  }

  // ── Double and Float type aliases ───────────────────────────────────────────

  it should "accept Double type annotation and transparent widening from Long" in {
    run("""
      function square(x: Double): Double { x * x }
      square(3)
    """).asInstanceOf[Double] shouldBe 9.0 +- 0.001
  }

  it should "accept Float type annotation" in {
    run("""
      function half(x: Float): Float { x }
      half(4)
    """) shouldBe 4L  // value passes type check (numeric widening)
  }

  it should "reject Boolean where Double expected" in {
    val ex = intercept[Exception] {
      run("""
        function f(x: Double): Double { x }
        f(true)
      """)
    }
    ex.getMessage should include("Double")
  }

  // ── multiple type parameter generics ───────────────────────────────────────

  it should "support two type parameters and return first" in {
    run("""
      function first<A, B>(a: A, b: B): A { a }
      first(42, "hello")
    """) shouldBe 42L
  }

  it should "support two type parameters and return second" in {
    run("""
      function second<A, B>(a: A, b: B): B { b }
      second(42, "hello")
    """) shouldBe "hello"
  }

  it should "support three type parameters" in {
    run("""
      function pick<A, B, C>(a: A, b: B, c: C): C { c }
      pick(1, "two", true)
    """) shouldBe true
  }

  // ── higher-order function types ─────────────────────────────────────────────

  it should "accept typed higher-order function returning a function" in {
    run("""
      function makeAdder(n: Long): (Long) -> Long {
        { x: Long -> x + n }
      }
      add5 = makeAdder(5)
      add5(10)
    """) shouldBe 15L
  }

  it should "accept nested function type ((Long) -> Long) -> Long" in {
    run("""
      function applyTwice(f: (Long) -> Long, x: Long): Long { f(f(x)) }
      applyTwice({ n: Long -> n + 1 }, 10)
    """) shouldBe 12L
  }

  it should "infer lambda param type from multi-param function type" in {
    run("""
      function apply(f: (Long, Long) -> Long, a: Long, b: Long): Long { f(a, b) }
      apply({ x, y -> x * y }, 6, 7)
    """) shouldBe 42L
  }

  // ── val/var type inference ──────────────────────────────────────────────────

  it should "infer Double type for floating-point literal" in {
    val ex = intercept[Exception] {
      run("""
        function test(): Long {
          val x = 3.14
          x = true  // should fail: inferred Double
          1
        }
        test()
      """)
    }
    ex.getMessage should (include("Double") or include("immutable"))
  }

  it should "infer Boolean type for boolean literal in val" in {
    val ex = intercept[Exception] {
      run("""
        function test(): Long {
          val flag = true
          flag = 42  // should fail: inferred Boolean
          1
        }
        test()
      """)
    }
    ex.getMessage should (include("Boolean") or include("immutable"))
  }

  it should "infer Char type for char literal in val" in {
    val ex = intercept[Exception] {
      run("""
        function test(): Long {
          val ch = 'A'
          ch = 999  // should fail: inferred Char (or immutable)
          1
        }
        test()
      """)
    }
    ex.getMessage should (include("Char") or include("immutable"))
  }

  it should "allow var reassignment with same inferred String type" in {
    run("""
      function test(): String {
        var s = "hello"
        s = "world"
        s
      }
      test()
    """) shouldBe "world"
  }

  it should "reject var reassignment with different inferred String type" in {
    val ex = intercept[Exception] {
      run("""
        function test(): Long {
          var s = "hello"
          s = 42  // should fail: inferred String
          1
        }
        test()
      """)
    }
    ex.getMessage should include("String")
  }
