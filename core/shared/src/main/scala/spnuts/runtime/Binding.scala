package spnuts.runtime

/**
 * Mutable binding for variable storage.
 * Closures capture Binding objects by reference, enabling mutation of captured variables.
 * This mirrors the original Pnuts `Binding` class.
 */
final class Binding(var value: Any):
  override def toString: String = s"Binding($value)"
