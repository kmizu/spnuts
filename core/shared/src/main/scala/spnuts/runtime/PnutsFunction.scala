package spnuts.runtime

import spnuts.ast.Expr

// ── Callable function types ────────────────────────────────────────────────────

/** Common trait for all callable function types. */
sealed trait AnyFunc:
  def name: Option[String]
  def nargs: Int
  def varargs: Boolean

/**
 * A single function overload with specific arity.
 * Mirrors the original pnuts.lang.Function.
 *
 * @param params       parameter names
 * @param varargs      if true, last param is a varargs array
 * @param body         AST body node
 * @param pkg          defining package
 * @param lexicalScope captured bindings at closure creation
 */
final class PnutsFunc(
  val name: Option[String],
  val params: Array[String],
  val varargs: Boolean,
  val body: Expr,
  val pkg: PnutsPackage,
  val lexicalScope: Map[String, Binding],
) extends AnyFunc:
  def nargs: Int = params.length

/**
 * A native (Scala/JVM) built-in function.
 * `impl` receives the evaluated arguments AND the current Context, so it can
 * call back into user-defined functions via Context.callFn.
 *
 * @param nargs  exact argument count; -1 means varargs
 */
final class NativeFunc(
  val name: Option[String],
  val nargs: Int,
  val varargs: Boolean,
  val impl: (Array[Any], Context) => Any,
) extends AnyFunc

object NativeFunc:
  /** Create a fixed-arity native function. */
  def apply(name: String, nargs: Int)(impl: (Array[Any], Context) => Any): NativeFunc =
    new NativeFunc(Some(name), nargs, false, impl)

  /** Create a varargs native function (nargs = -1). */
  def vararg(name: String)(impl: (Array[Any], Context) => Any): NativeFunc =
    new NativeFunc(Some(name), -1, true, impl)

/**
 * A group of function overloads indexed by arity (both Pnuts and native).
 * Mirrors the original pnuts.lang.PnutsFunction.
 */
final class PnutsGroup(val name: Option[String]):
  private val overloads = collection.mutable.HashMap.empty[Int, AnyFunc]
  private var varargFunc: Option[AnyFunc] = None
  // parent PnutsGroup in outer scope (for multi-scope overloading)
  var parent: Option[PnutsGroup] = None

  def register(f: AnyFunc): Unit =
    if f.varargs then varargFunc = Some(f)
    else overloads(f.nargs) = f

  def resolve(argc: Int): Option[AnyFunc] =
    overloads.get(argc)
      .orElse(varargFunc)
      .orElse(parent.flatMap(_.resolve(argc)))

/** Control-flow exceptions (mirrors original Jump / Break / Continue). */
class ReturnException(val value: Any)  extends Exception with scala.util.control.NoStackTrace
class BreakException(val value: Any)   extends Exception with scala.util.control.NoStackTrace
object ContinueException               extends Exception with scala.util.control.NoStackTrace
