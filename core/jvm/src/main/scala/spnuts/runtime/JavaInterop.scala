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

  // ── Class resolution cache ─────────────────────────────────────────────────

  private val classCache = mutable.HashMap.empty[String, Option[Class[?]]]

  def resolveClass(name: String, imports: List[String]): Option[Class[?]] =
    val candidates = buildCandidates(name, imports)
    candidates.flatMap { n =>
      classCache.getOrElseUpdate(n, try Some(Class.forName(n))
        catch case _: ClassNotFoundException => None)
    }.headOption

  private def buildCandidates(name: String, imports: List[String]): List[String] =
    if name.contains('.') then
      List(name) // fully qualified
    else
      name :: imports.flatMap { imp =>
        if imp == "*" then Nil
        else if imp.endsWith(".*") then List(imp.dropRight(1) + name)
        else if imp.endsWith(s".$name") then List(imp)
        else Nil
      } ::: List(
        s"java.lang.$name",
        s"java.util.$name",
      )

  // ── Method call ────────────────────────────────────────────────────────────

  def callMethod(cls: Class[?], target: Any, method: String, args: Array[Any], pos: SourcePos): Any =
    val candidates = getCandidateMethods(cls, method, args.length)
    val best = selectBest(candidates, args)
    best match
      case Some(m) =>
        m.setAccessible(true)
        val coerced = coerceArgs(m.getParameterTypes, args)
        try m.invoke(target, coerced*)
        catch
          case e: java.lang.reflect.InvocationTargetException =>
            throw e.getCause
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
        c.setAccessible(true)
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
      case Some(f) => f.setAccessible(true); f.get(target)
      case None =>
        val getter = s"get${member.capitalize}"
        findMethod(cls, getter, 0) match
          case Some(m) => m.setAccessible(true); m.invoke(target)
          case None =>
            // Boolean isX getter
            val isGetter = s"is${member.capitalize}"
            findMethod(cls, isGetter, 0) match
              case Some(m) => m.setAccessible(true); m.invoke(target)
              case None =>
                throw RuntimeError(s"No field or getter '$member' on ${cls.getName}", pos)

  def getStaticField(cls: Class[?], member: String, pos: SourcePos): Any =
    findField(cls, member) match
      case Some(f) => f.setAccessible(true); f.get(null) // null = static
      case None =>
        // Try static method with 0 args (e.g. EnumClass.VALUE)
        findMethod(cls, member, 0) match
          case Some(m) => m.setAccessible(true); m.invoke(null)
          case None =>
            throw RuntimeError(s"No static field or method '$member' on ${cls.getName}", pos)

  def setField(target: Any, member: String, value: Any, pos: SourcePos): Unit =
    val cls = target.getClass
    findField(cls, member) match
      case Some(f) => f.setAccessible(true); f.set(target, coerceValue(f.getType, value))
      case None =>
        val setter = s"set${member.capitalize}"
        findMethod(cls, setter, 1) match
          case Some(m) =>
            m.setAccessible(true)
            m.invoke(target, coerceValue(m.getParameterTypes()(0), value))
          case None =>
            throw RuntimeError(s"No field or setter '$member' on ${cls.getName}", pos)

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
      classOf[Byte], classOf[java.lang.Byte],
      classOf[Short], classOf[java.lang.Short],
      classOf[Int], classOf[java.lang.Integer],
      classOf[Long], classOf[java.lang.Long],
      classOf[Float], classOf[java.lang.Float],
      classOf[Double], classOf[java.lang.Double],
    )
    val ti = order.indexWhere(_ == target)
    val si = order.indexWhere(_ == src)
    if ti < 0 || si < 0 then -1
    else if ti >= si then (ti - si) / 2           // widening (preferred)
    else (si - ti) / 2 + 10                       // narrowing (allowed but penalized)

  private def isBoxedNumber(cls: Class[?]): Boolean =
    cls == classOf[java.lang.Integer] || cls == classOf[java.lang.Long] ||
    cls == classOf[java.lang.Double] || cls == classOf[java.lang.Float] ||
    cls == classOf[java.lang.Short] || cls == classOf[java.lang.Byte]

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
      spnuts.runtime.Operators.toLong(value)
    else if target == classOf[Int] || target == classOf[java.lang.Integer] then
      spnuts.runtime.Operators.toLong(value).toInt
    else if target == classOf[Double] || target == classOf[java.lang.Double] then
      spnuts.runtime.Operators.toDouble(value)
    else if target == classOf[Float] || target == classOf[java.lang.Float] then
      spnuts.runtime.Operators.toDouble(value).toFloat
    else if target == classOf[String] then
      spnuts.runtime.Operators.toStr(value)
    else value

  private def typeOf(v: Any): String = if v == null then "null" else v.getClass.getSimpleName
  private def toDouble(v: Any): Double = spnuts.runtime.Operators.toDouble(v)
