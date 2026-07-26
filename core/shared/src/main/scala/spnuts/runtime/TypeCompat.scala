package spnuts.runtime

/**
 * Numeric type compatibility for transparent boxing/unboxing (Scala/Kotlin style).
 *
 * SPnuts integer literals are Long at runtime; float literals are Double.
 * When a user annotates `n: Int` they mean "integer value" — so Long satisfies Int,
 * Short satisfies Int, etc. (widening is accepted implicitly).
 *
 * Rules:
 *   - Any integer type (Byte, Short, Char, Int, Long) satisfies any integer annotation.
 *   - Any float/double type satisfies any float annotation; integers also satisfy float (widening).
 *   - Char is compatible with integer types (Java: char → int widening).
 *   - Boolean and Unit are exact-match only (Boolean accepts Boolean; Unit accepts anything).
 *   - All other types require exact isInstance check.
 */
object TypeCompat:

  private val integerWrappers: Set[Class[?]] = Set(
    classOf[java.lang.Byte],
    classOf[java.lang.Short],
    classOf[java.lang.Character],  // Char is an unsigned 16-bit integer in Java
    classOf[java.lang.Integer],
    classOf[java.lang.Long],
  )

  private val floatWrappers: Set[Class[?]] = Set(
    classOf[java.lang.Float],
    classOf[java.lang.Double],
  )

  private val numericWrappers: Set[Class[?]] = integerWrappers ++ floatWrappers

  private val primitiveSemanticTypes: Set[Class[?]] =
    numericWrappers ++ Set(
      classOf[java.lang.Boolean],
      java.lang.Byte.TYPE,
      java.lang.Short.TYPE,
      java.lang.Character.TYPE,
      java.lang.Integer.TYPE,
      java.lang.Long.TYPE,
      java.lang.Float.TYPE,
      java.lang.Double.TYPE,
      java.lang.Boolean.TYPE,
    )

  /**
   * Check whether `value` is compatible with the declared type `cls`.
   * Transparent widening: any integer type satisfies any integer annotation, etc.
   */
  def isCompatible(cls: Class[?], value: Any): Boolean =
    if cls == classOf[scala.runtime.BoxedUnit] then true  // Unit accepts anything (void)
    else if value == null then !primitiveSemanticTypes.contains(cls)
    else if cls.isInstance(value) then true
    else
      val vc = value.getClass
      // integer ↔ integer widening (includes Char)
      (integerWrappers.contains(cls) && integerWrappers.contains(vc)) ||
      // numeric (int or float) → float widening
      (floatWrappers.contains(cls) && numericWrappers.contains(vc))

  /** Store a compatible numeric value in the representation promised by `cls`. */
  def coerce(cls: Class[?], value: Any): Any =
    if value == null then null
    else if cls == classOf[java.lang.Double] || cls == java.lang.Double.TYPE then
      value match
        case number: Number => number.doubleValue()
        case char: Character => char.charValue().toDouble
        case _ => value
    else if cls == classOf[java.lang.Float] || cls == java.lang.Float.TYPE then
      value match
        case number: Number => number.floatValue()
        case char: Character => char.charValue().toFloat
        case _ => value
    else value

  /** Human-readable type name using SPnuts alias names. */
  def typeName(cls: Class[?]): String =
    if cls == classOf[java.lang.Integer]            then "Int"
    else if cls == classOf[java.lang.Long]          then "Long"
    else if cls == classOf[java.lang.Short]         then "Short"
    else if cls == classOf[java.lang.Byte]          then "Byte"
    else if cls == classOf[java.lang.Float]         then "Float"
    else if cls == classOf[java.lang.Double]        then "Double"
    else if cls == classOf[java.lang.Character]     then "Char"
    else if cls == classOf[java.lang.Boolean]       then "Boolean"
    else if cls == classOf[scala.runtime.BoxedUnit] then "Unit"
    else cls.getSimpleName
