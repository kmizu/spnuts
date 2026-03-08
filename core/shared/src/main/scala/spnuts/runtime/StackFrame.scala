package spnuts.runtime

/**
 * Call-stack frame. Each function call creates a new StackFrame.
 * Within a frame, openScope/closeScope manage block-level scopes.
 *
 * Mirrors the original Pnuts StackFrame + SymbolTable interaction.
 */
final class StackFrame(
  val outer: Option[StackFrame] = None,        // enclosing function's frame (for closures)
  val lexicalScope: Map[String, Binding] = Map.empty, // captured bindings at closure creation
):
  private var currentScope: SymbolTable = SymbolTable()

  /** Open a new block scope (e.g., for-loop body). */
  def openScope(): Unit =
    currentScope = SymbolTable(Some(currentScope))

  /** Close the current block scope. */
  def closeScope(): Unit =
    currentScope = currentScope.parent.getOrElse(currentScope)

  /**
   * Declare a local variable in the current block scope.
   * Used when a new variable is assigned at function level.
   */
  def declare(name: String, value: Any): Binding =
    currentScope.declare(name, value)

  /** Declare a typed variable with optional immutability and static type. */
  def declareTyped(name: String, value: Any, immutable: Boolean, staticType: Option[Class[?]] = None): Binding =
    currentScope.declareTyped(name, value, immutable, staticType)

  /**
   * Look up a variable:
   *  1. Current scope chain (local vars + block scopes)
   *  2. Lexical scope (captured by closure)
   */
  def lookup(name: String): Option[Binding] =
    currentScope.lookup(name).orElse(lexicalScope.get(name))

  /**
   * Capture current visible bindings for closure creation.
   * Returns a snapshot of all bindings reachable from this frame.
   */
  def makeLexicalScope(): Map[String, Binding] =
    val captured = collection.mutable.HashMap.empty[String, Binding]
    // current frame
    captured ++= currentScope.flatten()
    // walk outer frames (lexical scope chain)
    var frame = outer
    while frame.isDefined do
      for (k, v) <- frame.get.lexicalScope if !captured.contains(k) do
        captured(k) = v
      frame = frame.get.outer
    captured.toMap
