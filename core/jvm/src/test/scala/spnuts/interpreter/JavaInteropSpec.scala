package spnuts.interpreter

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import spnuts.parser.Parser
import spnuts.runtime.{Context, BuiltinModule, JvmPlatform, PnutsPackage}

class JavaInteropSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll:

  override def beforeAll(): Unit =
    JvmPlatform.init()
    BuiltinModule.install(PnutsPackage.global)

  def run(src: String): Any =
    val ast = Parser.parse(src, "<test>")
    // Use a fresh child package so test assignments don't pollute PnutsPackage.global
    val pkg = PnutsPackage("javaInteropTest", Some(PnutsPackage.global))
    val ctx = Context(currentPackage = pkg)
    Interpreter.eval(ast, ctx)

  // ── new expression ──────────────────────────────────────────────────────────

  "JavaInterop" should "create java.util.ArrayList with new" in {
    run("""
      list = new java.util.ArrayList()
      list
    """).getClass.getName shouldBe "java.util.ArrayList"
  }

  it should "create java.util.HashMap with new" in {
    run("""
      m = new java.util.HashMap()
      m
    """).getClass.getName shouldBe "java.util.HashMap"
  }

  it should "create java.lang.StringBuilder with new and constructor arg" in {
    run("""
      sb = new java.lang.StringBuilder("hello")
      sb
    """).getClass.getName shouldBe "java.lang.StringBuilder"
  }

  // ── method calls ───────────────────────────────────────────────────────────

  it should "call methods on java.util.ArrayList" in {
    run("""
      list = new java.util.ArrayList()
      list.add("a")
      list.add("b")
      list.add("c")
      list.size()
    """) shouldBe 3
  }

  it should "call get() on ArrayList" in {
    run("""
      list = new java.util.ArrayList()
      list.add(42)
      list.get(0)
    """) shouldBe 42L
  }

  it should "call methods on java.lang.String" in {
    run("""
      s = "Hello, World!"
      s.length()
    """) shouldBe 13
  }

  it should "call String.toUpperCase()" in {
    run("""
      "hello".toUpperCase()
    """) shouldBe "HELLO"
  }

  it should "call String.substring()" in {
    run("""
      "Hello, World!".substring(7)
    """) shouldBe "World!"
  }

  it should "call String.contains()" in {
    run("""
      "Hello, World!".contains("World")
    """) shouldBe true
  }

  it should "call String.replace()" in {
    run("""
      "foo bar foo".replace("foo", "baz")
    """) shouldBe "baz bar baz"
  }

  it should "call String.split()" in {
    val result = run("""
      "a,b,c".split(",")
    """)
    result.asInstanceOf[Array[String]].toList shouldBe List("a", "b", "c")
  }

  // ── field access ───────────────────────────────────────────────────────────

  it should "access static field via qualified class name" in {
    run("""
      java.lang.Integer.MAX_VALUE
    """) shouldBe Int.MaxValue
  }

  // ── java.util.HashMap ──────────────────────────────────────────────────────

  it should "put and get values in HashMap" in {
    run("""
      m = new java.util.HashMap()
      m.put("key", "value")
      m.get("key")
    """) shouldBe "value"
  }

  it should "check HashMap containsKey" in {
    run("""
      m = new java.util.HashMap()
      m.put("x", 1)
      m.containsKey("x")
    """) shouldBe true
  }

  // ── java.lang.Math ─────────────────────────────────────────────────────────

  it should "call java.lang.Math.abs()" in {
    run("""
      java.lang.Math.abs(-42)
    """) shouldBe 42L
  }

  it should "call java.lang.Math.max()" in {
    run("""
      java.lang.Math.max(10, 20)
    """) shouldBe 20L
  }

  it should "call java.lang.Math.sqrt()" in {
    run("""
      java.lang.Math.sqrt(9.0)
    """) shouldBe 3.0
  }

  // ── instanceof ─────────────────────────────────────────────────────────────

  it should "evaluate instanceof for ArrayList" in {
    run("""
      list = new java.util.ArrayList()
      list instanceof java.util.ArrayList
    """) shouldBe true
  }

  it should "evaluate instanceof false for wrong type" in {
    run("""
      s = "hello"
      s instanceof java.util.ArrayList
    """) shouldBe false
  }

  // ── StringBuilder chaining ─────────────────────────────────────────────────

  it should "use StringBuilder to build a string" in {
    run("""
      sb = new java.lang.StringBuilder()
      sb.append("Hello")
      sb.append(", ")
      sb.append("World!")
      sb.toString()
    """) shouldBe "Hello, World!"
  }

  // ── Iterating over Java collections ────────────────────────────────────────

  it should "iterate over Java ArrayList with for-each" in {
    run("""
      list = new java.util.ArrayList()
      list.add(1)
      list.add(2)
      list.add(3)
      sum = 0
      for (x : list) sum = sum + x
      sum
    """) shouldBe 6L
  }

  // ── Type conversion ────────────────────────────────────────────────────────

  it should "auto-box primitives when calling Java methods" in {
    run("""
      list = new java.util.ArrayList()
      list.add(1)
      list.add(2)
      list.add(3)
      list.size()
    """) shouldBe 3
  }

  // ── Exception handling ─────────────────────────────────────────────────────

  it should "catch Java exceptions with try-catch" in {
    run("""
      try {
        list = new java.util.ArrayList()
        list.get(99)
        "no error"
      } catch (java.lang.IndexOutOfBoundsException e) {
        "caught"
      }
    """) shouldBe "caught"
  }

  it should "catch NumberFormatException" in {
    run("""
      try {
        java.lang.Integer.parseInt("not-a-number")
        "no error"
      } catch (java.lang.NumberFormatException e) {
        "caught: " + e.getMessage()
      }
    """).asInstanceOf[String] should startWith("caught:")
  }

  // ── Static method calls ────────────────────────────────────────────────────

  it should "call Integer.parseInt()" in {
    run("""
      java.lang.Integer.parseInt("42")
    """) shouldBe 42
  }

  it should "call String.valueOf()" in {
    run("""
      java.lang.String.valueOf(42)
    """) shouldBe "42"
  }

  it should "call Collections.sort() on ArrayList" in {
    run("""
      list = new java.util.ArrayList()
      list.add(3)
      list.add(1)
      list.add(2)
      java.util.Collections.sort(list)
      list.get(0)
    """) shouldBe 1
  }
