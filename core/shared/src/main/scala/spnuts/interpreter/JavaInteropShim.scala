package spnuts.interpreter

import spnuts.ast.SourcePos

/** Represents an unresolved dotted class/package path like "java" or "java.lang". */
case class ClassPathMarker(path: String)

/**
 * Shim for Java interop calls.
 * The default (shared) implementation is stub-only.
 * The JVM platform overrides this via JavaInterop in core/jvm/.
 *
 * This indirection keeps the shared interpreter free of JVM-only imports.
 */
object JavaInteropShim:

  /** Call an instance or static method via reflection. */
  def callMethod(cls: Class[?], target: Any, method: String, args: Array[Any], pos: SourcePos): Any =
    callMethodImpl(cls, target, method, args, pos)

  /** Get a field or property. */
  def getField(target: Any, member: String, pos: SourcePos): Any =
    getFieldImpl(target, member, pos)

  /** Get a static field from a class. */
  def getStaticField(cls: Class[?], member: String, pos: SourcePos): Any =
    getStaticFieldImpl(cls, member, pos)

  /** Set a field or property. */
  def setField(target: Any, member: String, value: Any, pos: SourcePos): Unit =
    setFieldImpl(target, member, value, pos)

  /** Resolve a class name using the import list. */
  def resolveClass(name: String, imports: List[String]): Option[Class[?]] =
    resolveClassImpl(name, imports)

  /** Call a constructor. */
  def callConstructor(cls: Class[?], args: Array[Any], pos: SourcePos): Any =
    callConstructorImpl(cls, args, pos)

  /** Create a new array of the given element type and length. */
  def newArray(elemCls: Class[?], size: Int): AnyRef =
    newArrayImpl(elemCls, size)

  /** Instantiate a class using its no-arg constructor. */
  def newInstance(cls: Class[?]): AnyRef =
    newInstanceImpl(cls)

  /** Get the array class for an element type (e.g. Long → Long[]). */
  def arrayClass(elemCls: Class[?]): Class[?] =
    arrayClassImpl(elemCls)

  // ── Mutable dispatch hooks (replaced by platform-specific init) ────────────

  var callMethodImpl: (Class[?], Any, String, Array[Any], SourcePos) => Any =
    defaultCallMethod
  var getFieldImpl: (Any, String, SourcePos) => Any =
    defaultGetField
  var getStaticFieldImpl: (Class[?], String, SourcePos) => Any =
    defaultGetStaticField
  var setFieldImpl: (Any, String, Any, SourcePos) => Unit =
    defaultSetField
  var resolveClassImpl: (String, List[String]) => Option[Class[?]] =
    defaultResolveClass
  var callConstructorImpl: (Class[?], Array[Any], SourcePos) => Any =
    defaultCallConstructor
  var newArrayImpl: (Class[?], Int) => AnyRef =
    defaultNewArray
  var newInstanceImpl: (Class[?]) => AnyRef =
    defaultNewInstance
  var arrayClassImpl: (Class[?]) => Class[?] =
    defaultArrayClass

  // ── Default stubs ──────────────────────────────────────────────────────────

  private def defaultCallMethod(cls: Class[?], target: Any, method: String, args: Array[Any], pos: SourcePos): Any =
    throw RuntimeError(s"Java interop not available: ${cls.getName}.$method", pos)

  private def defaultGetField(target: Any, member: String, pos: SourcePos): Any =
    throw RuntimeError(s"Java interop not available: field '$member'", pos)

  private def defaultGetStaticField(cls: Class[?], member: String, pos: SourcePos): Any =
    throw RuntimeError(s"Java interop not available: static field '${cls.getName}.$member'", pos)

  private def defaultSetField(target: Any, member: String, value: Any, pos: SourcePos): Unit =
    throw RuntimeError(s"Java interop not available: field '$member'", pos)

  // Statically-referenced classes available on both JVM and Scala Native.
  // Used by defaultResolveClass to handle common types without Class.forName.
  private val builtinClassMap: Map[String, Class[?]] = Map(
    "java.lang.Object"                   -> classOf[java.lang.Object],
    "java.lang.String"                   -> classOf[java.lang.String],
    "java.lang.Throwable"                -> classOf[java.lang.Throwable],
    "java.lang.Exception"                -> classOf[java.lang.Exception],
    "java.lang.RuntimeException"         -> classOf[java.lang.RuntimeException],
    "java.lang.Error"                    -> classOf[java.lang.Error],
    "java.lang.IllegalArgumentException" -> classOf[java.lang.IllegalArgumentException],
    "java.lang.IllegalStateException"    -> classOf[java.lang.IllegalStateException],
    "java.lang.NullPointerException"     -> classOf[java.lang.NullPointerException],
    "java.lang.UnsupportedOperationException" -> classOf[java.lang.UnsupportedOperationException],
    "java.lang.ClassCastException"       -> classOf[java.lang.ClassCastException],
    "java.lang.ArithmeticException"      -> classOf[java.lang.ArithmeticException],
    "java.lang.IndexOutOfBoundsException" -> classOf[java.lang.IndexOutOfBoundsException],
    "java.lang.ArrayIndexOutOfBoundsException" -> classOf[java.lang.ArrayIndexOutOfBoundsException],
    "java.lang.StackOverflowError"       -> classOf[java.lang.StackOverflowError],
    "java.lang.Boolean"                  -> classOf[java.lang.Boolean],
    "java.lang.Integer"                  -> classOf[java.lang.Integer],
    "java.lang.Long"                     -> classOf[java.lang.Long],
    "java.lang.Short"                    -> classOf[java.lang.Short],
    "java.lang.Byte"                     -> classOf[java.lang.Byte],
    "java.lang.Float"                    -> classOf[java.lang.Float],
    "java.lang.Double"                   -> classOf[java.lang.Double],
    "java.lang.Character"                -> classOf[java.lang.Character],
    "scala.runtime.BoxedUnit"            -> classOf[scala.runtime.BoxedUnit],
    // Common java.util types
    "java.util.List"                     -> classOf[java.util.List[?]],
    "java.util.ArrayList"                -> classOf[java.util.ArrayList[?]],
    "java.util.Map"                      -> classOf[java.util.Map[?, ?]],
    "java.util.HashMap"                  -> classOf[java.util.HashMap[?, ?]],
    "java.util.Set"                      -> classOf[java.util.Set[?]],
    "java.util.Collection"               -> classOf[java.util.Collection[?]],
    "java.util.Iterator"                 -> classOf[java.util.Iterator[?]],
  )

  private def defaultResolveClass(name: String, imports: List[String]): Option[Class[?]] =
    // Try FQN first, then common prefixes — using static map (no Class.forName for portability)
    val candidates = name :: (if name.contains('.') then Nil else List(
      s"java.lang.$name", s"java.util.$name"))
    candidates.flatMap(builtinClassMap.get).headOption

  private def defaultCallConstructor(cls: Class[?], args: Array[Any], pos: SourcePos): Any =
    throw RuntimeError(s"Java interop not available: constructor for ${cls.getName}", pos)

  private def defaultNewArray(elemCls: Class[?], size: Int): AnyRef =
    throw new UnsupportedOperationException(s"Java interop not available: array creation")

  private def defaultNewInstance(cls: Class[?]): AnyRef =
    throw new UnsupportedOperationException(s"Java interop not available: new ${cls.getName}()")

  private def defaultArrayClass(elemCls: Class[?]): Class[?] =
    throw new UnsupportedOperationException(s"Java interop not available: array class for ${elemCls.getName}")
