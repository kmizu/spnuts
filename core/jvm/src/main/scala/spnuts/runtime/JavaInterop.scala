package spnuts.runtime

import spnuts.ast.SourcePos
import spnuts.interpreter.{JavaInteropShim, RuntimeError}
import java.lang.reflect.{Method, Field, Constructor}
import scala.collection.mutable

/**
 * Full Java interop for the JVM platform.
 * Mirrors the overload resolution logic in the original Runtime._callMethod.
 *
 * Method resolution:
 *  1. Collect all methods matching the name
 *  2. Filter by argument count (exact or varargs)
 *  3. Score each by type compatibility (matchType distance)
 *  4. Select the best match (lowest score)
 */
object JavaInterop:

  /** Install this implementation as the shim. Called by JvmPlatform.init(). */
  def install(): Unit =
    JavaInteropShim.callMethodImpl      = callMethod
    JavaInteropShim.getFieldImpl        = getField
    JavaInteropShim.getStaticFieldImpl  = getStaticField
    JavaInteropShim.setFieldImpl        = setField
    JavaInteropShim.resolveClassImpl    = resolveClass
    JavaInteropShim.callConstructorImpl = callConstructor
    JavaInteropShim.newArrayImpl        = (cls, size) => java.lang.reflect.Array.newInstance(cls, size)
    JavaInteropShim.newInstanceImpl     = cls => cls.getDeclaredConstructor().newInstance()
    JavaInteropShim.arrayClassImpl      = cls => java.lang.reflect.Array.newInstance(cls, 0).getClass

  // ── Class resolution cache ─────────────────────────────────────────────────

  private val classCache = mutable.HashMap.empty[String, Option[Class[?]]]

  def resolveClass(name: String, imports: List[String]): Option[Class[?]] =
    val candidates = buildCandidates(name, imports)
    candidates.flatMap { n =>
      classCache.getOrElseUpdate(n, try Some(Class.forName(n))
        catch case _: ClassNotFoundException => None)
    }.headOption

  /**
   * Short type alias → fully-qualified JVM class name.
   * Handles cases where the JVM name differs from the SPnuts alias
   * (Int→Integer, Char→Character, Unit→scala.runtime.BoxedUnit).
   */
  private val typeAliasToFqn: Map[String, String] = Map(
    "Int"     -> "java.lang.Integer",
    "Char"    -> "java.lang.Character",
    "Unit"    -> "scala.runtime.BoxedUnit",
  )

  private def buildCandidates(name: String, imports: List[String]): List[String] =
    if name.contains('.') then
      List(name) // fully qualified
    else
      // Type-alias resolution first (Int→Integer, Char→Character)
      val aliasResolved = typeAliasToFqn.get(name).toList
      aliasResolved ::: (name :: imports.flatMap { imp =>
        if imp == "*" then Nil
        else if imp.endsWith(".*") then List(imp.dropRight(1) + name)
        else if imp.endsWith(s".$name") then List(imp)
        else Nil
      } ::: List(
        s"java.lang.$name",
        s"java.util.$name",
      ))

  // ── Method call ────────────────────────────────────────────────────────────

  def callMethod(cls: Class[?], target: Any, method: String, args: Array[Any], pos: SourcePos): Any =
    val candidates = getCandidateMethods(cls, method, args.length)
    val best = selectBest(candidates, args)
    best match
      case Some(m) =>
        m.trySetAccessible()  // ignore failure — public methods on public classes work without it
        val coerced = coerceArgs(m.getParameterTypes, args)
        try m.invoke(target, coerced*)
        catch
          case e: java.lang.reflect.InvocationTargetException =>
            throw e.getCause
          case _: IllegalAccessException =>
            // Java module encapsulation: declaring class is package-private (e.g. Collections$Unmodifiable*).
            // Find the same method via a public interface or superclass and invoke that.
            findAccessibleVersion(m, target.getClass) match
              case Some(am) =>
                try am.invoke(target, coerced*)
                catch case e: java.lang.reflect.InvocationTargetException => throw e.getCause
              case None =>
                throw RuntimeError(s"Method '${cls.getName}.${m.getName}' is inaccessible (Java module encapsulation)", pos)
      case None =>
        throw RuntimeError(
          s"No matching method '${cls.getName}.$method' for args (${args.map(typeOf).mkString(", ")})", pos)

  private def getCandidateMethods(cls: Class[?], name: String, argc: Int): Array[Method] =
    (cls.getMethods ++ cls.getDeclaredMethods)
      .filter(m => m.getName == name &&
                   (m.getParameterCount == argc || (m.isVarArgs && argc >= m.getParameterCount - 1)))
      .distinctBy(_.toString)

  // ── Constructor call ───────────────────────────────────────────────────────

  def callConstructor(cls: Class[?], args: Array[Any], pos: SourcePos): Any =
    val candidates = cls.getConstructors.filter { c =>
      c.getParameterCount == args.length ||
      (c.isVarArgs && args.length >= c.getParameterCount - 1)
    }
    val best = selectBestCtor(candidates, args)
    best match
      case Some(c) =>
        c.trySetAccessible()
        val coerced = coerceArgs(c.getParameterTypes, args)
        try c.newInstance(coerced*)
        catch
          case e: java.lang.reflect.InvocationTargetException => throw e.getCause
      case None =>
        // Try no-arg constructor
        cls.getDeclaredConstructor().newInstance()

  // ── Field access ───────────────────────────────────────────────────────────

  def getField(target: Any, member: String, pos: SourcePos): Any =
    val cls = target.getClass
    // Try field first, then getter method
    findField(cls, member) match
      case Some(f) => f.trySetAccessible(); f.get(target)
      case None =>
        val getter = s"get${member.capitalize}"
        findMethod(cls, getter, 0) match
          case Some(m) => m.trySetAccessible(); m.invoke(target)
          case None =>
            // Boolean isX getter
            val isGetter = s"is${member.capitalize}"
            findMethod(cls, isGetter, 0) match
              case Some(m) => m.trySetAccessible(); m.invoke(target)
              case None =>
                throw RuntimeError(s"No field or getter '$member' on ${cls.getName}", pos)

  def getStaticField(cls: Class[?], member: String, pos: SourcePos): Any =
    findField(cls, member) match
      case Some(f) => f.trySetAccessible(); f.get(null) // null = static
      case None =>
        // Try static method with 0 args (e.g. EnumClass.VALUE)
        findMethod(cls, member, 0) match
          case Some(m) => m.trySetAccessible(); m.invoke(null)
          case None =>
            throw RuntimeError(s"No static field or method '$member' on ${cls.getName}", pos)

  def setField(target: Any, member: String, value: Any, pos: SourcePos): Unit =
    val cls = target.getClass
    findField(cls, member) match
      case Some(f) => f.trySetAccessible(); f.set(target, coerceValue(f.getType, value))
      case None =>
        val setter = s"set${member.capitalize}"
        findMethod(cls, setter, 1) match
          case Some(m) =>
            m.trySetAccessible()
            m.invoke(target, coerceValue(m.getParameterTypes()(0), value))
          case None =>
            throw RuntimeError(s"No field or setter '$member' on ${cls.getName}", pos)

  /**
   * Find an accessible version of a method when the declaring class is package-private
   * (e.g. Collections$UnmodifiableCollection). Walks public interfaces and superclasses.
   */
  private def findAccessibleVersion(m: Method, runtimeCls: Class[?]): Option[Method] =
    val name   = m.getName
    val params = m.getParameterTypes
    def search(c: Class[?]): Option[Method] =
      if c == null then None
      else if java.lang.reflect.Modifier.isPublic(c.getModifiers) then
        try Some(c.getMethod(name, params*)) catch case _: NoSuchMethodException => searchParents(c)
      else searchParents(c)
    def searchParents(c: Class[?]): Option[Method] =
      c.getInterfaces.iterator.flatMap(search).nextOption()
        .orElse(search(c.getSuperclass))
    search(runtimeCls)

  private def findField(cls: Class[?], name: String): Option[Field] =
    try Some(cls.getField(name))
    catch case _: NoSuchFieldException =>
      try Some(cls.getDeclaredField(name))
      catch case _: NoSuchFieldException => None

  private def findMethod(cls: Class[?], name: String, argc: Int): Option[Method] =
    (cls.getMethods ++ cls.getDeclaredMethods)
      .find(m => m.getName == name && m.getParameterCount == argc)

  // ── Type scoring (matches original Runtime.matchType) ─────────────────────

  /** Score how well `value` matches `paramType`. Lower = better. -1 = incompatible. */
  private def matchType(paramType: Class[?], value: Any): Int =
    if value == null then
      if paramType.isPrimitive then -1 else 0
    else if paramType.isInstance(value) then 0
    else if paramType.isPrimitive || isBoxedNumber(paramType) then
      numericDistance(paramType, value.getClass)
    else -1

  private def numericDistance(target: Class[?], src: Class[?]): Int =
    val order = List(
      classOf[Byte],  classOf[java.lang.Byte],
      classOf[Short], classOf[java.lang.Short],
      classOf[Char],  classOf[java.lang.Character],
      classOf[Int],   classOf[java.lang.Integer],
      classOf[Long],  classOf[java.lang.Long],
      classOf[Float], classOf[java.lang.Float],
      classOf[Double],classOf[java.lang.Double],
    )
    val ti = order.indexWhere(_ == target)
    val si = order.indexWhere(_ == src)
    if ti < 0 || si < 0 then -1
    else if ti >= si then (ti - si) / 2           // widening (preferred)
    else (si - ti) / 2 + 10                       // narrowing (allowed but penalized)

  private def isBoxedNumber(cls: Class[?]): Boolean =
    cls == classOf[java.lang.Integer]   || cls == classOf[java.lang.Long]    ||
    cls == classOf[java.lang.Double]    || cls == classOf[java.lang.Float]   ||
    cls == classOf[java.lang.Short]     || cls == classOf[java.lang.Byte]    ||
    cls == classOf[java.lang.Character]

  private def scoreMethod(paramTypes: Array[Class[?]], args: Array[Any]): Int =
    if paramTypes.length != args.length then return Int.MaxValue
    val scores = paramTypes.zip(args).map((t, a) => matchType(t, a))
    if scores.contains(-1) then Int.MaxValue else scores.sum

  private def selectBest(candidates: Array[Method], args: Array[Any]): Option[Method] =
    candidates.map(m => m -> scoreMethod(m.getParameterTypes, args))
              .filter(_._2 < Int.MaxValue)
              .sortBy(_._2)
              .headOption
              .map(_._1)

  private def selectBestCtor(candidates: Array[Constructor[?]], args: Array[Any]): Option[Constructor[?]] =
    candidates.map(c => c -> scoreMethod(c.getParameterTypes, args))
              .filter(_._2 < Int.MaxValue)
              .sortBy(_._2)
              .headOption
              .map(_._1)

  // ── Argument coercion ──────────────────────────────────────────────────────

  private def coerceArgs(paramTypes: Array[Class[?]], args: Array[Any]): Array[AnyRef] =
    paramTypes.zip(args).map((t, a) => coerceValue(t, a).asInstanceOf[AnyRef])

  private def coerceValue(target: Class[?], value: Any): Any =
    if target == classOf[Long] || target == classOf[java.lang.Long] then
      Operators.toLong(value)
    else if target == classOf[Int] || target == classOf[java.lang.Integer] then
      Operators.toLong(value).toInt
    else if target == classOf[Short] || target == classOf[java.lang.Short] then
      Operators.toLong(value).toShort
    else if target == classOf[Byte] || target == classOf[java.lang.Byte] then
      Operators.toLong(value).toByte
    else if target == classOf[Double] || target == classOf[java.lang.Double] then
      Operators.toDouble(value)
    else if target == classOf[Float] || target == classOf[java.lang.Float] then
      Operators.toDouble(value).toFloat
    else if target == classOf[Char] || target == classOf[java.lang.Character] then
      value match
        case c: java.lang.Character => c
        case _                      => Operators.toLong(value).toChar
    else if target == classOf[Boolean] || target == classOf[java.lang.Boolean] then
      value.asInstanceOf[java.lang.Boolean]
    else if target == classOf[String] then
      Operators.toStr(value)
    else value

  private def typeOf(v: Any): String = if v == null then "null" else v.getClass.getSimpleName
  private def toDouble(v: Any): Double = spnuts.runtime.Operators.toDouble(v)
