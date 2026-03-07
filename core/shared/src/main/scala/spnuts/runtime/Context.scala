package spnuts.runtime

import java.io.{PrintWriter, Writer}

/**
 * Execution context. Carries all mutable state for one script execution.
 * Mirrors the original pnuts.lang.Context.
 *
 * Cloned for nested eval()/load() calls (currentPackage + importEnv reset).
 */
final class Context(
  var currentPackage: PnutsPackage = PnutsPackage.global,
  var writer: PrintWriter          = new PrintWriter(System.out, true),
  var errorWriter: PrintWriter     = new PrintWriter(System.err, true),
):
  var stackFrame: Option[StackFrame] = None

  /**
   * Buffer for yield values accumulated during a generator call.
   * Set to a new ArrayList at the start of a function call, null when not in a generator.
   */
  var yieldBuf: java.util.ArrayList[Any] = null

  /**
   * Callback into the interpreter for calling user-defined functions.
   * Set by Interpreter.eval before first evaluation.
   * Built-in NativeFuncs use this to invoke PnutsGroup/PnutsFunc arguments.
   */
  var callFn: (Any, Array[Any], Context, spnuts.ast.SourcePos) => Any = null

  // Import environment: list of imported package prefixes
  val imports: collection.mutable.ListBuffer[String] =
    collection.mutable.ListBuffer("java.lang.*")

  /**
   * Resolve a variable:
   *  1. Current stack frame (local + lexical scope)
   *  2. Current package (+ parent chain)
   *  3. Global package
   */
  def getValue(name: String): Any =
    stackFrame.flatMap(_.lookup(name)).map(_.value)
      .orElse(currentPackage.lookup(name).map(_.value))
      .orElse(PnutsPackage.global.lookup(name).map(_.value))
      .getOrElse(throw new RuntimeException(s"Undefined variable: $name"))

  /**
   * Set a variable:
   *  1. If in a function: update existing binding or declare new local
   *  2. At top level: set in current package
   */
  def setValue(name: String, value: Any): Unit =
    stackFrame match
      case Some(frame) =>
        frame.lookup(name) match
          case Some(b) => b.value = value
          case None    => frame.declare(name, value)
      case None =>
        currentPackage.set(name, value)

  /**
   * Open a new function call frame.
   * @param func    the function being called
   * @param args    evaluated arguments
   * @param outerFrame the caller's frame (for closures)
   */
  def openFrame(func: PnutsFunc, args: Array[Any]): StackFrame =
    val frame = StackFrame(outer = stackFrame, lexicalScope = func.lexicalScope)
    // Bind parameters
    for i <- 0 until func.params.length do
      if func.varargs && i == func.params.length - 1 then
        // Pack remaining args into an array
        val rest = args.drop(i)
        frame.declare(func.params(i), rest)
      else
        val v = if i < args.length then args(i) else null
        frame.declare(func.params(i), v)
    stackFrame = Some(frame)
    frame

  /** Close the current function call frame. */
  def closeFrame(savedFrame: Option[StackFrame]): Unit =
    stackFrame = savedFrame

  /** Open a new block scope within the current frame. */
  def openScope(): Unit = stackFrame.foreach(_.openScope())

  /** Close the current block scope. */
  def closeScope(): Unit = stackFrame.foreach(_.closeScope())

  /** Shallow clone for eval()/load() — resets package + imports. */
  def cloneForEval(): Context =
    val c = Context(PnutsPackage.global, writer, errorWriter)
    c
