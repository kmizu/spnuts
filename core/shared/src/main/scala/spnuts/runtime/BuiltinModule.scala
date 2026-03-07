package spnuts.runtime

import spnuts.ast.SourcePos
import scala.collection.mutable
import java.util.{ArrayList, LinkedHashMap}

/**
 * Core built-in functions, mirroring pnuts.lib.
 * Registered in PnutsPackage.global at startup.
 *
 * Functions are NativeFunc instances so they work on both JVM and Native.
 */
object BuiltinModule:

  private val noPos = SourcePos("<builtin>", 0, 0)

  def install(pkg: PnutsPackage): Unit =
    // Get or create the PnutsGroup for `name`, so multiple arities accumulate.
    def getOrCreateGroup(name: String): PnutsGroup =
      pkg.lookup(name).map(_.value) match
        case Some(g: PnutsGroup) => g
        case _ =>
          val g = PnutsGroup(Some(name))
          pkg.set(name, g)
          g

    def reg(name: String, nargs: Int)(impl: (Array[Any], Context) => Any): Unit =
      getOrCreateGroup(name).register(NativeFunc(name, nargs)(impl))

    def regV(name: String)(impl: (Array[Any], Context) => Any): Unit =
      getOrCreateGroup(name).register(NativeFunc.vararg(name)(impl))

    // ── I/O ───────────────────────────────────────────────────────────────────

    regV("print") { (args, _) =>
      print(args.map(Operators.toStr).mkString(""))
      null
    }

    regV("println") { (args, _) =>
      if args.isEmpty then println()
      else println(args.map(Operators.toStr).mkString(""))
      null
    }

    reg("p", 1) { (args, _) =>
      val v = args(0)
      println(Operators.toStr(v))
      v
    }

    // ── Type conversion ───────────────────────────────────────────────────────

    reg("str", 1) { (args, _) => Operators.toStr(args(0)) }

    reg("int", 1) { (args, _) =>
      args(0) match
        case n: Long    => n
        case n: Int     => n.toLong
        case n: Double  => n.toLong
        case n: Float   => n.toLong
        case s: String  => s.toLong
        case b: Boolean => if b then 1L else 0L
        case v          => Operators.toLong(v)
    }

    reg("float", 1) { (args, _) =>
      args(0) match
        case n: Double  => n
        case n: Float   => n.toDouble
        case n: Long    => n.toDouble
        case n: Int     => n.toDouble
        case s: String  => s.toDouble
        case v          => Operators.toDouble(v)
    }

    reg("boolean", 1) { (args, _) => Operators.toBoolean(args(0)) }

    reg("char", 1) { (args, _) =>
      args(0) match
        case c: Char   => c
        case n: Long   => n.toChar
        case s: String if s.nonEmpty => s.charAt(0)
        case v         => Operators.toLong(v).toChar
    }

    // ── Reflection / type info ────────────────────────────────────────────────

    reg("typeOf", 1) { (args, _) =>
      val v = args(0)
      if v == null then "null" else v.getClass.getSimpleName
    }

    reg("isNull", 1)   { (args, _) => args(0) == null }
    reg("isString", 1) { (args, _) => args(0).isInstanceOf[String] }
    reg("isArray", 1)  { (args, _) => args(0).isInstanceOf[Array[?]] }

    reg("isNumber", 1) { (args, _) =>
      args(0) match
        case _: Long | _: Double | _: Int | _: Float => true
        case _ => false
    }

    // ── Collection creation ────────────────────────────────────────────────────

    reg("array", 1) { (args, _) =>
      val n = Operators.toLong(args(0)).toInt
      new Array[Any](n)
    }

    regV("list") { (args, _) =>
      val al = new ArrayList[Any]()
      for a <- args do al.add(a)
      al
    }

    reg("map", 0) { (_, _) => new LinkedHashMap[Any, Any]() }

    // ── Collection operations ─────────────────────────────────────────────────

    reg("size", 1) { (args, _) =>
      args(0) match
        case arr: Array[?]                => arr.length.toLong
        case s: String                    => s.length.toLong
        case col: java.util.Collection[?] => col.size.toLong
        case m: java.util.Map[?, ?]       => m.size.toLong
        case null                         => 0L
        case v => throw new RuntimeException(s"size: not a collection: $v")
    }

    reg("length", 1) { (args, _) =>
      args(0) match
        case arr: Array[?]                => arr.length.toLong
        case s: String                    => s.length.toLong
        case col: java.util.Collection[?] => col.size.toLong
        case v => throw new RuntimeException(s"length: not supported for $v")
    }

    reg("isEmpty", 1) { (args, _) =>
      args(0) match
        case arr: Array[?]                => arr.isEmpty
        case s: String                    => s.isEmpty
        case col: java.util.Collection[?] => col.isEmpty
        case m: java.util.Map[?, ?]       => m.isEmpty
        case null                         => true
        case _                            => false
    }

    // ── Higher-order collection functions ─────────────────────────────────────

    reg("map", 2) { (args, ctx) =>
      val (col, fn) = (args(0), args(1))
      val result = new ArrayList[Any]()
      foreachItem(col) { item =>
        result.add(ctx.callFn(fn, Array(item), ctx, noPos))
      }
      result
    }

    reg("filter", 2) { (args, ctx) =>
      val (col, fn) = (args(0), args(1))
      val result = new ArrayList[Any]()
      foreachItem(col) { item =>
        if Operators.toBoolean(ctx.callFn(fn, Array(item), ctx, noPos)) then result.add(item)
      }
      result
    }

    reg("reduce", 3) { (args, ctx) =>
      val (col, fn, init) = (args(0), args(1), args(2))
      var acc: Any = init
      foreachItem(col) { item => acc = ctx.callFn(fn, Array(acc, item), ctx, noPos) }
      acc
    }

    reg("each", 2) { (args, ctx) =>
      val (col, fn) = (args(0), args(1))
      foreachItem(col) { item => ctx.callFn(fn, Array(item), ctx, noPos) }
      null
    }

    reg("any", 2) { (args, ctx) =>
      val (col, fn) = (args(0), args(1))
      var found = false
      foreachItem(col) { item =>
        if !found && Operators.toBoolean(ctx.callFn(fn, Array(item), ctx, noPos)) then found = true
      }
      found
    }

    reg("all", 2) { (args, ctx) =>
      val (col, fn) = (args(0), args(1))
      var allTrue = true
      foreachItem(col) { item =>
        if allTrue && !Operators.toBoolean(ctx.callFn(fn, Array(item), ctx, noPos)) then allTrue = false
      }
      allTrue
    }

    reg("sort", 1) { (args, _) =>
      args(0) match
        case arr: Array[Any] =>
          arr.sortWith((a, b) => Operators.lt(a, b).asInstanceOf[Boolean])
        case col: java.util.List[Any] @unchecked =>
          val arr = col.toArray().asInstanceOf[Array[Any]]
          val sorted = arr.sortWith((a, b) => Operators.lt(a, b).asInstanceOf[Boolean])
          col.clear()
          for item <- sorted do col.add(item)
          col
        case v => throw new RuntimeException(s"sort: not a list: $v")
    }

    reg("sort", 2) { (args, ctx) =>
      val (col, fn) = (args(0), args(1))
      col match
        case arr: Array[Any] =>
          arr.sortWith((a, b) => Operators.toBoolean(ctx.callFn(fn, Array(a, b), ctx, noPos)))
        case lst: java.util.List[Any] @unchecked =>
          val arr = lst.toArray().asInstanceOf[Array[Any]]
          val sorted = arr.sortWith((a, b) => Operators.toBoolean(ctx.callFn(fn, Array(a, b), ctx, noPos)))
          lst.clear()
          for item <- sorted do lst.add(item)
          lst
        case v => throw new RuntimeException(s"sort: not a list: $v")
    }

    reg("reverse", 1) { (args, _) =>
      args(0) match
        case arr: Array[Any] => arr.reverse
        case lst: java.util.List[Any] @unchecked =>
          java.util.Collections.reverse(lst)
          lst
        case v => throw new RuntimeException(s"reverse: not a list: $v")
    }

    reg("append", 2) { (args, _) =>
      args(0) match
        case lst: java.util.List[Any] @unchecked =>
          lst.add(args(1))
          lst
        case v => throw new RuntimeException(s"append: not a list: $v")
    }

    reg("get", 2) { (args, _) =>
      args(0) match
        case m: java.util.Map[Any, Any] @unchecked => m.get(args(1))
        case arr: Array[?]                         => arr(Operators.toLong(args(1)).toInt)
        case lst: java.util.List[Any] @unchecked   => lst.get(Operators.toLong(args(1)).toInt)
        case v => throw new RuntimeException(s"get: not indexable: $v")
    }

    reg("put", 3) { (args, _) =>
      args(0) match
        case m: java.util.Map[Any, Any] @unchecked =>
          m.put(args(1), args(2))
          m
        case v => throw new RuntimeException(s"put: not a map: $v")
    }

    reg("keys", 1) { (args, _) =>
      args(0) match
        case m: java.util.Map[Any, Any] @unchecked =>
          m.keySet().toArray().asInstanceOf[Array[Any]]
        case v => throw new RuntimeException(s"keys: not a map: $v")
    }

    reg("values", 1) { (args, _) =>
      args(0) match
        case m: java.util.Map[Any, Any] @unchecked =>
          m.values().toArray().asInstanceOf[Array[Any]]
        case v => throw new RuntimeException(s"values: not a map: $v")
    }

    reg("contains", 2) { (args, _) =>
      args(0) match
        case col: java.util.Collection[Any] @unchecked => col.contains(args(1))
        case m: java.util.Map[Any, ?] @unchecked       => m.containsKey(args(1))
        case arr: Array[Any] @unchecked                => arr.contains(args(1))
        case s: String => s.contains(Operators.toStr(args(1)))
        case v => throw new RuntimeException(s"contains: not a collection: $v")
    }

    // ── Range / sequence ──────────────────────────────────────────────────────

    reg("range", 2) { (args, _) =>
      val from = Operators.toLong(args(0))
      val to   = Operators.toLong(args(1))
      val n    = (to - from).toInt max 0
      val arr  = new Array[Any](n)
      for i <- 0 until n do arr(i) = from + i
      arr
    }

    reg("range", 3) { (args, _) =>
      val from = Operators.toLong(args(0))
      val to   = Operators.toLong(args(1))
      val step = Operators.toLong(args(2))
      val buf  = mutable.ArrayBuffer.empty[Any]
      var i = from
      while i < to do { buf += i; i += step }
      buf.toArray
    }

    // ── String operations ─────────────────────────────────────────────────────

    reg("join", 2) { (args, _) =>
      val sep = Operators.toStr(args(1))
      args(0) match
        case arr: Array[?]          => arr.map(Operators.toStr).mkString(sep)
        case lst: java.util.List[?] => lst.toArray.map(Operators.toStr).mkString(sep)
        case v => throw new RuntimeException(s"join: not a list: $v")
    }

    reg("split", 2) { (args, _) =>
      val s   = Operators.toStr(args(0))
      val sep = Operators.toStr(args(1))
      s.split(java.util.regex.Pattern.quote(sep)).asInstanceOf[Array[Any]]
    }

    reg("trim", 1)        { (args, _) => Operators.toStr(args(0)).trim }
    reg("toUpperCase", 1) { (args, _) => Operators.toStr(args(0)).toUpperCase }
    reg("toLowerCase", 1) { (args, _) => Operators.toStr(args(0)).toLowerCase }

    reg("startsWith", 2) { (args, _) =>
      Operators.toStr(args(0)).startsWith(Operators.toStr(args(1)))
    }

    reg("endsWith", 2) { (args, _) =>
      Operators.toStr(args(0)).endsWith(Operators.toStr(args(1)))
    }

    reg("indexOf", 2) { (args, _) =>
      Operators.toStr(args(0)).indexOf(Operators.toStr(args(1))).toLong
    }

    reg("substring", 3) { (args, _) =>
      val s     = Operators.toStr(args(0))
      val start = Operators.toLong(args(1)).toInt
      val end   = Operators.toLong(args(2)).toInt
      s.substring(start, end)
    }

    reg("substring", 2) { (args, _) =>
      val s     = Operators.toStr(args(0))
      val start = Operators.toLong(args(1)).toInt
      s.substring(start)
    }

    reg("replace", 3) { (args, _) =>
      Operators.toStr(args(0))
        .replace(Operators.toStr(args(1)), Operators.toStr(args(2)))
    }

    reg("format", -1) { (args, _) =>
      val fmt  = Operators.toStr(args(0))
      val rest = args.tail
      String.format(fmt, rest.map(_.asInstanceOf[AnyRef])*)
    }

    // ── Math ──────────────────────────────────────────────────────────────────

    reg("abs", 1) { (args, _) =>
      args(0) match
        case n: Long   => math.abs(n)
        case n: Double => math.abs(n)
        case v         => math.abs(Operators.toLong(v))
    }

    reg("max", 2) { (args, _) =>
      if Operators.gt(args(0), args(1)).asInstanceOf[Boolean] then args(0) else args(1)
    }

    reg("min", 2) { (args, _) =>
      if Operators.lt(args(0), args(1)).asInstanceOf[Boolean] then args(0) else args(1)
    }

    reg("pow", 2) { (args, _) =>
      math.pow(Operators.toDouble(args(0)), Operators.toDouble(args(1)))
    }

    reg("sqrt", 1)  { (args, _) => math.sqrt(Operators.toDouble(args(0))) }
    reg("floor", 1) { (args, _) => math.floor(Operators.toDouble(args(0))).toLong }
    reg("ceil", 1)  { (args, _) => math.ceil(Operators.toDouble(args(0))).toLong }
    reg("round", 1) { (args, _) => math.round(Operators.toDouble(args(0))) }

    // ── Misc ──────────────────────────────────────────────────────────────────

    reg("sleep", 1) { (args, _) =>
      Thread.sleep(Operators.toLong(args(0)))
      null
    }

    reg("assert", 1) { (args, _) =>
      if !Operators.toBoolean(args(0)) then throw new AssertionError("Assertion failed")
      null
    }

    reg("assert", 2) { (args, _) =>
      if !Operators.toBoolean(args(0)) then throw new AssertionError(Operators.toStr(args(1)))
      null
    }

    reg("error", 1) { (args, _) =>
      throw new RuntimeException(Operators.toStr(args(0)))
    }

    // ── Type aliases ──────────────────────────────────────────────────────────

    reg("type", 1) { (args, _) =>
      val v = args(0)
      if v == null then "null" else v.getClass.getSimpleName
    }

    // ── Generator ─────────────────────────────────────────────────────────────

    /** generator(fn, args...) — call fn collecting yield values into an array */
    regV("generator") { (args, ctx) =>
      if args.isEmpty then throw new RuntimeException("generator: requires at least 1 argument")
      val fn      = args(0)
      val fnArgs  = args.drop(1)
      ctx.callFn(fn, fnArgs, ctx, noPos)
      // callFn on a generator function returns the yield-accumulated array
    }

    // ── Eval ──────────────────────────────────────────────────────────────────

    reg("eval", 1) { (args, ctx) =>
      val code = Operators.toStr(args(0))
      spnuts.parser.Parser.parse(code, "<eval>") match
        case ast =>
          // Use a fresh context cloned from current
          val evalCtx = ctx.cloneForEval()
          evalCtx.callFn = ctx.callFn
          spnuts.interpreter.Interpreter.eval(ast, evalCtx)
    }

    // ── String / collection extras ────────────────────────────────────────────

    regV("concat") { (args, _) =>
      args.map(Operators.toStr).mkString("")
    }

    reg("charAt", 2) { (args, _) =>
      val s = Operators.toStr(args(0))
      val i = Operators.toLong(args(1)).toInt
      s.charAt(i)
    }

    reg("toArray", 1) { (args, _) =>
      args(0) match
        case arr: Array[?]               => arr
        case lst: java.util.List[Any] @unchecked => lst.toArray()
        case v => throw new RuntimeException(s"toArray: not a list: $v")
    }

    reg("toList", 1) { (args, _) =>
      args(0) match
        case arr: Array[Any] @unchecked =>
          val al = new java.util.ArrayList[Any]()
          arr.foreach(al.add)
          al
        case lst: java.util.List[?] => lst
        case v => throw new RuntimeException(s"toList: not a list: $v")
    }

    reg("flatten", 1) { (args, _) =>
      val result = new java.util.ArrayList[Any]()
      foreachItem(args(0)) { item =>
        item match
          case inner: Array[?] => inner.foreach(result.add)
          case inner: java.util.List[?] =>
            val it = inner.iterator()
            while it.hasNext do result.add(it.next())
          case v => result.add(v)
      }
      result
    }

    reg("first", 1) { (args, _) =>
      args(0) match
        case arr: Array[?]                  => if arr.nonEmpty then arr(0) else null
        case lst: java.util.List[?]         => if !lst.isEmpty then lst.get(0) else null
        case v => throw new RuntimeException(s"first: not a list: $v")
    }

    reg("last", 1) { (args, _) =>
      args(0) match
        case arr: Array[?]          => if arr.nonEmpty then arr(arr.length - 1) else null
        case lst: java.util.List[?] => if !lst.isEmpty then lst.get(lst.size - 1) else null
        case v => throw new RuntimeException(s"last: not a list: $v")
    }

    reg("remove", 2) { (args, _) =>
      args(0) match
        case lst: java.util.List[Any] @unchecked =>
          lst.remove(Operators.toLong(args(1)).toInt)
        case m: java.util.Map[Any, ?] @unchecked =>
          m.remove(args(1))
        case v => throw new RuntimeException(s"remove: not supported for $v")
    }

    reg("copy", 1) { (args, _) =>
      args(0) match
        case arr: Array[Any] @unchecked => arr.clone()
        case lst: java.util.List[Any] @unchecked =>
          new java.util.ArrayList[Any](lst)
        case m: java.util.Map[Any, Any] @unchecked =>
          new java.util.LinkedHashMap[Any, Any](m)
        case v => throw new RuntimeException(s"copy: not supported for $v")
    }

    // ── Math extras ───────────────────────────────────────────────────────────

    reg("log",  1) { (args, _) => math.log(Operators.toDouble(args(0))) }
    reg("log10",1) { (args, _) => math.log10(Operators.toDouble(args(0))) }
    reg("sin",  1) { (args, _) => math.sin(Operators.toDouble(args(0))) }
    reg("cos",  1) { (args, _) => math.cos(Operators.toDouble(args(0))) }
    reg("tan",  1) { (args, _) => math.tan(Operators.toDouble(args(0))) }
    reg("PI",   0) { (_, _)    => math.Pi }
    reg("E",    0) { (_, _)    => math.E }
    reg("random", 0) { (_, _)  => math.random() }

    // ── Regex ─────────────────────────────────────────────────────────────────

    reg("matches", 2) { (args, _) =>
      Operators.toStr(args(0)).matches(Operators.toStr(args(1)))
    }

    reg("replaceAll", 3) { (args, _) =>
      Operators.toStr(args(0))
        .replaceAll(Operators.toStr(args(1)), Operators.toStr(args(2)))
    }

  // ── Private helpers ────────────────────────────────────────────────────────

  /** Iterate over any collection-like value. */
  private def foreachItem(col: Any)(f: Any => Unit): Unit =
    col match
      case arr: Array[?]               => arr.foreach(f)
      case lst: java.util.List[?]      =>
        val it = lst.iterator()
        while it.hasNext do f(it.next())
      case iter: java.lang.Iterable[?] =>
        val it = iter.iterator()
        while it.hasNext do f(it.next())
      case v => throw new RuntimeException(s"Not iterable: $v")
