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

  // ── Numeric coercion (boxing/unboxing transparency) ─────────────────────────

  it should "coerce Long to int when calling Java method expecting int" in {
    // String.charAt(int) — SPnuts passes Long, JavaInterop must coerce to int
    run("""
      s = "hello"
      s.charAt(1)
    """) shouldBe 'e'
  }

  it should "coerce Long to Integer when calling java.lang.Integer method" in {
    run("""
      java.lang.Integer.toBinaryString(10)
    """) shouldBe "1010"
  }

  it should "coerce Long to short for Short.valueOf" in {
    run("""
      java.lang.Short.valueOf(42)
    """) shouldBe (42: Short)
  }

  it should "coerce Long to byte for Byte.valueOf" in {
    run("""
      java.lang.Byte.valueOf(127)
    """) shouldBe (127: Byte)
  }

  it should "pass Char to Java method expecting char" in {
    // Character.isLetter(char)
    run("""
      java.lang.Character.isLetter('A')
    """) shouldBe true
  }

  it should "pass Char to Java method expecting int (widening)" in {
    // Character.getNumericValue(int) — char widens to int
    run("""
      java.lang.Character.getNumericValue('9')
    """) shouldBe 9
  }

  it should "coerce Long to double for Math.sqrt" in {
    run("""
      java.lang.Math.sqrt(9)
    """) shouldBe 3.0
  }

  it should "coerce Long to float for Float.valueOf" in {
    run("""
      java.lang.Float.valueOf(2)
    """) shouldBe (2.0f)
  }

  it should "resolve Int as class alias for java.lang.Integer" in {
    // Using short name 'Integer' (via java.lang.* auto-import) in class creation
    run("""
      java.lang.Integer.MAX_VALUE
    """) shouldBe java.lang.Integer.MAX_VALUE
  }

  // ── Array interop ────────────────────────────────────────────────────────────

  it should "create and use a Long array via reflection" in {
    run("""
      import java.lang.reflect.*
      arr = Array.newInstance(java.lang.Long, 3)
      arr[0] = 10
      arr[1] = 20
      arr[2] = 30
      arr[0]
    """) shouldBe 10L
  }

  it should "pass array to Java method (Arrays.fill)" in {
    run("""
      import java.util.Arrays
      import java.lang.reflect.Array
      arr = Array.newInstance(java.lang.Long, 3)
      Arrays.fill(arr, 7)
      arr[0]
    """) shouldBe 7L
  }

  // ── Boolean params / returns ─────────────────────────────────────────────────

  it should "call Java method with boolean param (String.startsWith)" in {
    run("""
      "hello".startsWith("hel")
    """) shouldBe true
  }

  it should "call Java method returning boolean (String.isEmpty)" in {
    run("""
      "".isEmpty()
    """) shouldBe true
  }

  it should "call Java method returning boolean (ArrayList.isEmpty)" in {
    run("""
      list = new java.util.ArrayList()
      list.isEmpty()
    """) shouldBe true
  }

  it should "negate boolean return from Java method" in {
    run("""
      !"".isEmpty()
    """) shouldBe false
  }

  // ── Char return from Java methods ────────────────────────────────────────────

  it should "retrieve char from String.charAt and use in equality" in {
    run("""
      c = "hello".charAt(0)
      c == 'h'
    """) shouldBe true
  }

  it should "call Character.isUpperCase on Java char result" in {
    run("""
      c = "Hello".charAt(0)
      java.lang.Character.isUpperCase(c)
    """) shouldBe true
  }

  it should "call Character.toLowerCase on a char" in {
    run("""
      java.lang.Character.toLowerCase('A')
    """) shouldBe 'a'
  }

  // ── String method coercions ──────────────────────────────────────────────────

  it should "call String.indexOf(int) with Long arg (coercion)" in {
    run("""
      "abcde".indexOf('c')
    """) shouldBe 2
  }

  it should "call String.substring(int, int) with Long args" in {
    run("""
      "hello world".substring(6, 11)
    """) shouldBe "world"
  }

  it should "call String.repeat(int) with Long arg" in {
    run("""
      "ab".repeat(3)
    """) shouldBe "ababab"
  }

  // ── Overload resolution ──────────────────────────────────────────────────────

  it should "resolve best overload: Math.max(long, long)" in {
    run("""
      java.lang.Math.max(10, 20)
    """) shouldBe 20L
  }

  it should "resolve best overload: Math.max(double, double)" in {
    run("""
      java.lang.Math.max(1.5, 2.5)
    """) shouldBe 2.5
  }

  it should "resolve Math.min with Long args" in {
    run("""
      java.lang.Math.min(100, 42)
    """) shouldBe 42L
  }

  it should "resolve Math.pow(double, double) with Long args (widening)" in {
    run("""
      java.lang.Math.pow(2, 10)
    """) shouldBe 1024.0
  }

  // ── Constructor coercion ─────────────────────────────────────────────────────

  it should "construct Integer from Long arg" in {
    run("""
      new java.lang.Integer(42)
    """) shouldBe 42
  }

  it should "construct Long from Long arg" in {
    run("""
      new java.lang.Long(99)
    """) shouldBe 99L
  }

  it should "construct StringBuilder with Long (no-arg fallback)" in {
    // StringBuilder(int capacity) — Long coerces to int
    run("""
      sb = new java.lang.StringBuilder(16)
      sb.capacity()
    """) shouldBe 16
  }

  // ── null argument handling ────────────────────────────────────────────────────

  it should "pass null to String method (String.valueOf null)" in {
    run("""
      java.lang.String.valueOf(null)
    """) shouldBe "null"
  }

  // ── Primitive returns from Java methods ─────────────────────────────────────

  it should "auto-box int return from String.length() to Long" in {
    val result = run("""
      "hello".length()
    """)
    result shouldBe 5
  }

  it should "auto-box boolean return (usable in if)" in {
    run("""
      if ("abc".contains("b")) 1 else 0
    """) shouldBe 1L
  }

  it should "auto-box double return from Math.sqrt" in {
    run("""
      java.lang.Math.sqrt(16)
    """) shouldBe 4.0
  }

  it should "use int return from Java method in arithmetic" in {
    run("""
      "hello".length() + 1
    """) shouldBe 6
  }

  // ── Static vs instance methods ───────────────────────────────────────────────

  it should "call static method Integer.parseInt" in {
    run("""
      java.lang.Integer.parseInt("123")
    """) shouldBe 123
  }

  it should "call instance method on String result of static call" in {
    run("""
      java.lang.String.valueOf(42).length()
    """) shouldBe 2
  }

  it should "call static Math.abs on Long arg" in {
    run("""
      java.lang.Math.abs(-99)
    """) shouldBe 99L
  }

  // ── Collection methods with generics ────────────────────────────────────────

  it should "call ArrayList.add and get with type coercion" in {
    run("""
      list = new java.util.ArrayList()
      list.add("hello")
      list.add("world")
      list.get(1)
    """) shouldBe "world"
  }

  it should "call HashMap.put and get" in {
    run("""
      map = new java.util.HashMap()
      map.put("key", "value")
      map.get("key")
    """) shouldBe "value"
  }

  it should "call Collections.unmodifiableList" in {
    run("""
      list = new java.util.ArrayList()
      list.add(1)
      list.add(2)
      ul = java.util.Collections.unmodifiableList(list)
      ul.size()
    """) shouldBe 2
  }
