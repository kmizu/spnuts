package spnuts.runtime

/**
 * Mutable binding for variable storage.
 * Closures capture Binding objects by reference, enabling mutation of captured variables.
 * This mirrors the original Pnuts `Binding` class.
 *
 * @param immutable    if true, the binding cannot be reassigned (val semantics).
 * @param staticType   the statically-inferred or declared type of this binding.
 *                     If Some(cls), every subsequent assignment is type-checked against cls.
 */
final class Binding(
  var value: Any,
  val immutable: Boolean = false,
  val staticType: Option[Class[?]] = None,
):
  def set(newValue: Any, name: String): Unit =
    if immutable then throw new RuntimeException(s"Cannot reassign immutable variable '$name'")
    staticType.foreach { cls =>
      if !TypeCompat.isCompatible(cls, newValue) then
        throw new RuntimeException(
          s"Type error: variable '$name' has type ${TypeCompat.typeName(cls)} but assigned ${if newValue == null then "null" else TypeCompat.typeName(newValue.getClass)}")
    }
    value = newValue
  override def toString: String = s"Binding($value)"
