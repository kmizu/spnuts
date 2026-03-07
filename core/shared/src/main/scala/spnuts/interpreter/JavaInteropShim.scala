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

  // ── Default stubs ──────────────────────────────────────────────────────────

  private def defaultCallMethod(cls: Class[?], target: Any, method: String, args: Array[Any], pos: SourcePos): Any =
    throw RuntimeError(s"Java interop not available: ${cls.getName}.$method", pos)

  private def defaultGetField(target: Any, member: String, pos: SourcePos): Any =
    throw RuntimeError(s"Java interop not available: field '$member'", pos)

  private def defaultGetStaticField(cls: Class[?], member: String, pos: SourcePos): Any =
    throw RuntimeError(s"Java interop not available: static field '${cls.getName}.$member'", pos)

  private def defaultSetField(target: Any, member: String, value: Any, pos: SourcePos): Unit =
    throw RuntimeError(s"Java interop not available: field '$member'", pos)

  private def defaultResolveClass(name: String, imports: List[String]): Option[Class[?]] =
    // Basic: try java.lang.* without full interop
    val candidates = name :: imports.flatMap { imp =>
      if imp.endsWith(".*") then List(imp.dropRight(1) + name)
      else if imp.endsWith(s".$name") then List(imp)
      else Nil
    }
    candidates.flatMap { n =>
      try Some(Class.forName(n))
      catch case _: ClassNotFoundException => None
    }.headOption

  private def defaultCallConstructor(cls: Class[?], args: Array[Any], pos: SourcePos): Any =
    if args.isEmpty then
      cls.getDeclaredConstructor().newInstance()
    else
      throw RuntimeError(s"Java interop not available: constructor for ${cls.getName}", pos)
