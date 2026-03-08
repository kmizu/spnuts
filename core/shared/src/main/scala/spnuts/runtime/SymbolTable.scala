package spnuts.runtime

import scala.collection.mutable

/**
 * A hash-table scope that maps names to mutable Bindings.
 * Mirrors the original Pnuts SymbolTable.
 *
 * @param parent enclosing scope (for block-level scoping within a function)
 */
final class SymbolTable(val parent: Option[SymbolTable] = None):
  private val table = mutable.HashMap.empty[String, Binding]

  /** Declare a new variable in this scope. */
  def declare(name: String, value: Any): Binding =
    val b = Binding(value)
    table(name) = b
    b

  /** Declare a typed variable with optional immutability and static type. */
  def declareTyped(name: String, value: Any, immutable: Boolean, staticType: Option[Class[?]] = None): Binding =
    val b = Binding(value, immutable, staticType)
    table(name) = b
    b

  /** Look up a binding in this scope chain. */
  def lookup(name: String): Option[Binding] =
    table.get(name).orElse(parent.flatMap(_.lookup(name)))

  /** Set an existing binding's value; returns true if found. */
  def set(name: String, value: Any): Boolean =
    lookup(name) match
      case Some(b) => b.set(value, name); true
      case None    => false

  /** All bindings in this scope (not parent). */
  def localBindings: Map[String, Binding] = table.toMap

  /** Flatten all visible bindings (this + parent chain) for closure capture. */
  def flatten(): Map[String, Binding] =
    val result = mutable.HashMap.empty[String, Binding]
    var scope: Option[SymbolTable] = Some(this)
    while scope.isDefined do
      val s = scope.get
      for (k, v) <- s.table if !result.contains(k) do result(k) = v
      scope = s.parent
    result.toMap
