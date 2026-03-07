package spnuts.runtime

import scala.collection.mutable

/**
 * Pnuts package (namespace). Mirrors the original pnuts.lang.Package.
 *
 * Hierarchical: global package ("") is the root.
 * Named packages use "." as separator internally
 * (original uses "::", we normalize to ".").
 */
final class PnutsPackage(val name: String, val parent: Option[PnutsPackage] = None):
  private val symbols  = mutable.HashMap.empty[String, Binding]
  private val children = mutable.HashMap.empty[String, PnutsPackage]

  /** Look up a binding in this package, then parent chain. */
  def lookup(sym: String): Option[Binding] =
    symbols.get(sym).orElse(parent.flatMap(_.lookup(sym)))

  /** Set a value in this package (creates binding if absent). */
  def set(sym: String, value: Any): Unit =
    symbols.get(sym) match
      case Some(b) => b.value = value
      case None    => symbols(sym) = Binding(value)

  /** Get or create a child package. */
  def child(childName: String): PnutsPackage =
    children.getOrElseUpdate(childName, PnutsPackage(s"$name.$childName", Some(this)))

object PnutsPackage:
  /** The global root package (name = ""). */
  val global: PnutsPackage = PnutsPackage("")

  /** Pre-populate global with primitive types and basic functions. */
  def initGlobals(): Unit =
    // Primitive type aliases (mirrors Context.globals in original)
    val prims = Map(
      "int"     -> classOf[Int],
      "long"    -> classOf[Long],
      "float"   -> classOf[Float],
      "double"  -> classOf[Double],
      "boolean" -> classOf[Boolean],
      "byte"    -> classOf[Byte],
      "short"   -> classOf[Short],
      "char"    -> classOf[Char],
      "void"    -> classOf[Unit],
    )
    for (k, v) <- prims do global.set(k, v)
    // Install built-in functions (pnuts.lib)
    BuiltinModule.install(global)
