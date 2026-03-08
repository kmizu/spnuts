package spnuts.compiler

import spnuts.ast.SourcePos
import spnuts.interpreter.{Interpreter, JavaInteropShim, RuntimeError}
import spnuts.runtime.*

/**
 * Static helper methods called by compiled (JIT-generated) bytecode.
 *
 * Every method here is `static` (via Scala object) so generated code can call
 * them with `INVOKESTATIC spnuts/compiler/CompiledHelper$`.
 *
 * Naming convention: methods that mirror interpreter semantics exactly are
 * thin wrappers around Interpreter / JavaInteropShim helpers.
 */
object CompiledHelper:

  // ── Function / method call ─────────────────────────────────────────────────

  def callFunc(func: Any, args: Array[Object], ctx: Context,
               file: String, line: Int, col: Int): Any =
    val pos = SourcePos(file, line, col)
    Interpreter.callValuePublic(func, args.asInstanceOf[Array[Any]], ctx, pos)

  def callMethod(target: Any, method: String, args: Array[Object], ctx: Context,
                 file: String, line: Int, col: Int): Any =
    val pos = SourcePos(file, line, col)
    Interpreter.callMethodPublic(target, method, args.asInstanceOf[Array[Any]], ctx, pos)

  // ── Field / element access ─────────────────────────────────────────────────

  def getField(target: Any, member: String, ctx: Context,
               file: String, line: Int, col: Int): Any =
    val pos = SourcePos(file, line, col)
    Interpreter.getFieldPublic(target, member, ctx, pos)

  def setField(target: Any, member: String, value: Any, ctx: Context,
               file: String, line: Int, col: Int): Unit =
    JavaInteropShim.setField(target, member, value, SourcePos(file, line, col))

  def getElement(target: Any, idx: Any): Any =
    Interpreter.getElementPublic(target, idx, SourcePos("<compiled>", 0, 0))

  def getElement(target: Any, idx: Long): Any =
    getElement(target, idx.asInstanceOf[Any])

  def setElement(target: Any, idx: Any, value: Any): Unit =
    Interpreter.setElementPublic(target, idx, value, SourcePos("<compiled>", 0, 0))

  def getRange(target: Any, from: Any, to: Any): Any =
    Interpreter.getRangePublic(target, from, Option(to), SourcePos("<compiled>", 0, 0))

  // ── Global variable ────────────────────────────────────────────────────────

  def setGlobal(name: String, value: Any): Unit =
    PnutsPackage.global.set(name, value)

  // ── Collections ───────────────────────────────────────────────────────────

  def makeList(elems: Array[Object]): Any =
    val arr = new Array[Any](elems.length)
    System.arraycopy(elems, 0, arr, 0, elems.length)
    arr

  def makeMap(kvs: Array[Object]): Any =
    val m = new java.util.LinkedHashMap[Any, Any]()
    var i = 0
    while i < kvs.length - 1 do
      m.put(kvs(i), kvs(i + 1))
      i += 2
    m

  def makeRange(from: Any, to: Any): Any =
    val f = Operators.toLong(from)
    val t = Operators.toLong(to)
    val n = ((t - f) + 1).toInt max 0
    val arr = new Array[Any](n)
    for i <- 0 until n do arr(i) = f + i
    arr

  // ── Iteration ─────────────────────────────────────────────────────────────

  def makeIterator(col: Any): java.util.Iterator[?] = col match
    case arr: Array[?] => java.util.Arrays.asList(arr*).iterator()
    case it: java.lang.Iterable[?] => it.iterator()
    case _ => throw new RuntimeException(s"Cannot iterate over ${col.getClass.getSimpleName}")

  // ── Yield support ─────────────────────────────────────────────────────────

  def yield_(value: Any, ctx: Context): Unit =
    if ctx.yieldBuf == null then ctx.yieldBuf = new java.util.ArrayList[Any]()
    ctx.yieldBuf.add(value)

  // ── Exception handling ─────────────────────────────────────────────────────

  def throwValue(value: Any): Throwable = value match
    case t: Throwable => t
    case s: String    => new RuntimeException(s)
    case other        => new RuntimeException(Operators.toStr(other))

  // ── String interpolation ──────────────────────────────────────────────────

  def interpolate(parts: Array[Object]): String =
    val sb = new StringBuilder
    for p <- parts do
      p match
        case s: String => sb.append(s)
        case other     => sb.append(Operators.toStr(other))
    sb.toString

  // ── Java interop ──────────────────────────────────────────────────────────

  def newObject(ctx: Context, className: String, args: Array[Object],
                file: String, line: Int, col: Int): Any =
    val pos = SourcePos(file, line, col)
    val cls = JavaInteropShim.resolveClass(className, ctx.imports.toList)
      .getOrElse(throw RuntimeError(s"Cannot resolve class: $className", pos))
    JavaInteropShim.callConstructor(cls, args.asInstanceOf[Array[Any]], pos)

  def castValue(value: Any, ctx: Context, typeName: String,
                file: String, line: Int, col: Int): Any =
    val pos = SourcePos(file, line, col)
    val cls = JavaInteropShim.resolveClass(typeName, ctx.imports.toList)
      .getOrElse(throw RuntimeError(s"Cannot resolve class: $typeName", pos))
    Interpreter.castValuePublic(value, cls, pos)

  def instanceofCheck(value: Any, ctx: Context, typeName: String,
                      file: String, line: Int, col: Int): Any =
    val pos = SourcePos(file, line, col)
    val cls = JavaInteropShim.resolveClass(typeName, ctx.imports.toList)
      .getOrElse(throw RuntimeError(s"Cannot resolve class: $typeName", pos))
    Boolean.box(cls.isInstance(value))

  def resolveClass(ctx: Context, typeName: String,
                   file: String, line: Int, col: Int): Any =
    val pos = SourcePos(file, line, col)
    JavaInteropShim.resolveClass(typeName, ctx.imports.toList)
      .getOrElse(throw RuntimeError(s"Cannot resolve class: $typeName", pos))

  // ── Import / package ──────────────────────────────────────────────────────

  def addImport(ctx: Context, importStr: String): Unit =
    ctx.imports += importStr

  def setPackage(ctx: Context, pkgName: String): Unit =
    ctx.currentPackage = ctx.currentPackage.child(pkgName)

  // ── Record ────────────────────────────────────────────────────────────────

  def defineRecord(ctx: Context, name: String, fieldNames: Array[String]): Unit =
    val factory = PnutsRecord.makeFactory(name, fieldNames.toList)
    ctx.currentPackage.set(name, factory)

  // ── Switch ────────────────────────────────────────────────────────────────

  def execSwitch(target: Any, labelsAndBodies: Array[Object], ctx: Context,
                 file: String, line: Int, col: Int): Any =
    // labelsAndBodies: interleaved [label0, label1, ..., body] repeated per case
    // This is complex to do statically; delegate to interpreter's switch logic.
    // For the compiler, we generate simplified switch handling via this helper.
    null  // stub

  // ── Variable declaration ───────────────────────────────────────────────────

  def declareVar(ctx: Context, name: String, value: Any, immutable: Boolean): Any =
    val staticType: Option[Class[?]] = if value != null then Some(value.getClass) else None
    ctx.declareVar(name, value, immutable, staticType)
    value

  // ── Function stub ─────────────────────────────────────────────────────────

  def defineFuncStub(ctx: Context): Any =
    // FuncDef nodes are pre-registered by Compiler.compileScript before exec is called.
    null
