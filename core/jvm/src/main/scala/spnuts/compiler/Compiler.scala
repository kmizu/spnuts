package spnuts.compiler

import org.objectweb.asm.{ClassWriter, Label, Opcodes, MethodVisitor}
import org.objectweb.asm.Opcodes.*
import spnuts.ast.*
import spnuts.interpreter.Interpreter
import spnuts.runtime.ReturnException
import spnuts.runtime.*

/**
 * ASM-based JIT compiler for SPnuts.
 *
 * Compiles Pnuts AST nodes to JVM bytecode at runtime (in-memory class loading).
 *
 * Design:
 *  - Each script / function body compiles to a generated class with a static
 *    `exec(Context ctx): Object` method.
 *  - Variables use JVM local slots for function parameters; all other variables
 *    go through Context.getValue / Context.setValue.
 *  - Function definitions inside compiled code are handled by either:
 *      a) Compiling the inner function to its own generated class, or
 *      b) Falling back to PnutsFunc (interpreted) for closures.
 *  - Fallback: if a node type is not supported by CodeGen, the compiler throws
 *    UnsupportedOperationException and the caller can fall back to the interpreter.
 *
 * Entry points:
 *  - `Compiler.compileScript(exprs)`  → `Context => Any`
 *  - `Compiler.compileFunc(funcDef)`  → replaces body with compiled NativeFunc
 */
object Compiler:

  private val loader = new SpnutsClassLoader(Thread.currentThread().getContextClassLoader)
  private val counter = new java.util.concurrent.atomic.AtomicLong(0)

  // ── Public API ─────────────────────────────────────────────────────────────

  /**
   * Compile a top-level script (ExprList) to an executable thunk.
   * Returns a function `Context => Any`.
   *
   * Falls back to `None` if compilation is not possible.
   */
  def compileScript(exprs: ExprList, pkg: PnutsPackage): Option[Context => Any] =
    try
      val className = freshClassName("Script")
      val bytes = buildClass(className, Nil, exprs)
      val cls = loader.define(className.replace('/', '.'), bytes)
      val execMethod = cls.getMethod("exec", classOf[Context])
      Some(ctx => execMethod.invoke(null, ctx))
    catch
      case _: UnsupportedOperationException => None
      case e: Exception =>
        // Compilation failed for other reasons — log and fall back
        System.err.println(s"[spnuts JIT] compilation failed: ${e.getMessage}")
        None

  /**
   * Compile a single function definition.
   * Returns a NativeFunc that wraps the compiled bytecode, or None on failure.
   */
  def compileFunc(func: FuncDef, pkg: PnutsPackage,
                  lexical: Map[String, Binding]): Option[AnyFunc] =
    try
      val className = freshClassName(func.name.getOrElse("Lambda"))
      val bytes = buildClass(className, func.params, func.body)
      val cls = loader.define(className.replace('/', '.'), bytes)
      val execMethod = cls.getMethod("exec", classOf[Context], classOf[Array[Object]])
      val nargs = func.params.length
      val varargs = func.varargs
      val native = if varargs then
        NativeFunc.vararg(func.name.getOrElse("<lambda>")) { (args, ctx) =>
          val pkg2 = ctx.currentPackage
          ctx.currentPackage = pkg
          val savedFrame = ctx.stackFrame
          // push synthetic frame for param binding
          val frame = StackFrame(outer = Some(StackFrame(outer = None, lexicalScope = lexical)),
                                 lexicalScope = Map.empty)
          ctx.stackFrame = Some(frame)
          try
            val result = execMethod.invoke(null, ctx, args.asInstanceOf[Array[Object]])
            handleYield(result, ctx)
          catch
            case e: java.lang.reflect.InvocationTargetException => unwrapInvoke(e, ctx)
          finally
            ctx.stackFrame = savedFrame
            ctx.currentPackage = pkg2
        }
      else
        NativeFunc(func.name.getOrElse("<lambda>"), nargs) { (args, ctx) =>
          val pkg2 = ctx.currentPackage
          ctx.currentPackage = pkg
          val savedFrame = ctx.stackFrame
          val frame = StackFrame(outer = Some(StackFrame(outer = None, lexicalScope = lexical)),
                                 lexicalScope = Map.empty)
          ctx.stackFrame = Some(frame)
          try
            val result = execMethod.invoke(null, ctx, args.asInstanceOf[Array[Object]])
            handleYield(result, ctx)
          catch
            case e: java.lang.reflect.InvocationTargetException => unwrapInvoke(e, ctx)
          finally
            ctx.stackFrame = savedFrame
            ctx.currentPackage = pkg2
        }
      Some(native)
    catch
      case _: UnsupportedOperationException => None
      case e: Exception =>
        System.err.println(s"[spnuts JIT] function compilation failed: ${e.getMessage}")
        None

  // ── Class generation ───────────────────────────────────────────────────────

  /**
   * Generate a class with:
   *   public static Object exec(Context ctx)              — for scripts
   *   public static Object exec(Context ctx, Object[] args) — for functions
   */
  private def buildClass(className: String, params: List[String], body: Expr): Array[Byte] =
    val cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)
    cw.visit(V11, ACC_PUBLIC | ACC_SUPER, className, null, "java/lang/Object", null)

    val isFunc = params.nonEmpty || !body.isInstanceOf[ExprList]
    val scope = ScopeAnalyzer.analyze(params, body)

    if isFunc then
      buildFuncMethod(cw, className, scope, body)
    else
      buildScriptMethod(cw, className, scope, body)

    cw.visitEnd()
    cw.toByteArray

  /** Generate: `public static Object exec(Context ctx)` for scripts. */
  private def buildScriptMethod(cw: ClassWriter, className: String,
                                scope: ScopeAnalyzer.ScopeInfo, body: Expr): Unit =
    val mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "exec",
      "(L" + CodeGen.CTX_CLS + ";)Ljava/lang/Object;", null, null)
    mv.visitCode()
    val gen = new CodeGen(mv, scope)
    body match
      case ExprList(exprs, _) =>
        if exprs.isEmpty then mv.visitInsn(ACONST_NULL)
        else
          for e <- exprs.init do gen.compileStmt(e)
          gen.compileExpr(exprs.last)
      case _ =>
        gen.compileExpr(body)
    mv.visitInsn(ARETURN)
    mv.visitMaxs(0, 0)
    mv.visitEnd()

  /** Generate: `public static Object exec(Context ctx, Object[] args)` for functions. */
  private def buildFuncMethod(cw: ClassWriter, className: String,
                              scope: ScopeAnalyzer.ScopeInfo, body: Expr): Unit =
    val mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "exec",
      "(L" + CodeGen.CTX_CLS + ";[Ljava/lang/Object;)Ljava/lang/Object;", null, null)
    mv.visitCode()

    // Bind parameters from args[] into JVM local slots
    for (param, i) <- scope.params.zipWithIndex do
      mv.visitVarInsn(ALOAD, 1)  // args array is slot 1
      mv.visitLdcInsn(i)
      mv.visitInsn(AALOAD)
      mv.visitVarInsn(ASTORE, i + 2)  // params start at slot 2 (slot 0=ctx, slot 1=args[])
    // Note: scope.slotOf(param) gives slot i+1 (ctx=0, params=1..n).
    // But we're using slot 2..n+1 here because args[] occupies slot 1.
    // We need to adjust scope to offset by 1 more.

    // slotBase=1 shifts param slots by 1 to account for args[] at slot 1
    val gen = new CodeGen(mv, scope, slotBase = 1)
    body match
      case ExprList(exprs, _) =>
        if exprs.isEmpty then mv.visitInsn(ACONST_NULL)
        else
          for e <- exprs.init do gen.compileStmt(e)
          gen.compileExpr(exprs.last)
      case Block(exprs, _) =>
        if exprs.isEmpty then mv.visitInsn(ACONST_NULL)
        else
          for e <- exprs.init do gen.compileStmt(e)
          gen.compileExpr(exprs.last)
      case _ =>
        gen.compileExpr(body)
    mv.visitInsn(ARETURN)
    mv.visitMaxs(0, 0)
    mv.visitEnd()

  private def freshClassName(base: String): String =
    val n = counter.getAndIncrement()
    s"spnuts/compiled/Spnuts$$${base.replaceAll("[^A-Za-z0-9]", "_")}_$n"

  private def handleYield(result: Any, ctx: Context): Any =
    val buf = ctx.yieldBuf
    if buf != null && !buf.isEmpty then
      ctx.yieldBuf = null
      buf.toArray()
    else result

  private def unwrapInvoke(e: java.lang.reflect.InvocationTargetException, ctx: Context): Any =
    e.getCause match
      case re: ReturnException => re.value
      case t: Throwable        => throw t

// ── Class loader ──────────────────────────────────────────────────────────────

class SpnutsClassLoader(parent: ClassLoader) extends ClassLoader(parent):
  def define(name: String, bytes: Array[Byte]): Class[?] =
    defineClass(name, bytes, 0, bytes.length)

// ── FuncDef registry ──────────────────────────────────────────────────────────

/**
 * Global registry mapping integer IDs to FuncDef AST nodes.
 * Compiled code stores a FuncDef here at compile time and retrieves it at
 * runtime to create a PnutsFunc (with proper lexical-scope capture).
 */
object FuncDefRegistry:
  private val registry = new java.util.concurrent.ConcurrentHashMap[Int, spnuts.ast.FuncDef]()
  private val counter  = new java.util.concurrent.atomic.AtomicInteger(0)

  def register(fd: spnuts.ast.FuncDef): Int =
    val id = counter.getAndIncrement()
    registry.put(id, fd)
    id

  def get(id: Int): spnuts.ast.FuncDef = registry.get(id)
