package spnuts.runtime

/**
 * Arithmetic and comparison operators.
 * Mirrors the type promotion logic in the original Runtime.java + BinaryOperator.java.
 *
 * Pnuts follows Java's numeric promotion:
 *   int op int   -> int (or Long for Long suffixed)
 *   int op long  -> long
 *   int op float -> float (actually double in Java)
 *   * op double  -> double
 *   string + *   -> string (toString concatenation)
 */
object Operators:

  // ── Numeric helpers ────────────────────────────────────────────────────────

  private def isInt(v: Any): Boolean = v.isInstanceOf[Int] || v.isInstanceOf[java.lang.Integer]
  private def isLong(v: Any): Boolean = v.isInstanceOf[Long] || v.isInstanceOf[java.lang.Long]
  private def isFloat(v: Any): Boolean = v.isInstanceOf[Float] || v.isInstanceOf[java.lang.Float]
  private def isDouble(v: Any): Boolean = v.isInstanceOf[Double] || v.isInstanceOf[java.lang.Double]
  private def isBigInt(v: Any): Boolean = v.isInstanceOf[BigInt]

  def toLong(v: Any): Long = v match
    case i: Int    => i.toLong
    case l: Long   => l
    case i: java.lang.Integer => i.toLong
    case l: java.lang.Long    => l.toLong
    case b: BigInt => b.toLong
    case _         => throw new ClassCastException(s"Cannot convert $v to Long")

  def toDouble(v: Any): Double = v match
    case i: Int    => i.toDouble
    case l: Long   => l.toDouble
    case f: Float  => f.toDouble
    case d: Double => d
    case i: java.lang.Integer => i.toDouble
    case l: java.lang.Long    => l.toDouble
    case f: java.lang.Float   => f.toDouble
    case d: java.lang.Double  => d.toDouble
    case b: BigInt => b.toDouble
    case _         => throw new ClassCastException(s"Cannot convert $v to Double")

  /** Determine the "widest" numeric type in a pair and promote both. */
  private def promoteNumeric(a: Any, b: Any): (Any, Any) =
    if isDouble(a) || isDouble(b) then (toDouble(a), toDouble(b))
    else if isFloat(a) || isFloat(b) then (toDouble(a), toDouble(b))
    else if isLong(a) || isLong(b) then (toLong(a), toLong(b))
    else (toLong(a), toLong(b))

  // ── Binary operators ───────────────────────────────────────────────────────

  def add(a: Any, b: Any): Any = (a, b) match
    case (s: String, _)  => s + toStr(b)
    case (_, s: String)  => toStr(a) + s
    case _ =>
      promoteNumeric(a, b) match
        case (x: Double, y: Double) => x + y
        case (x: Long,   y: Long)   => x + y
        case _                      => toDouble(a) + toDouble(b)

  def sub(a: Any, b: Any): Any =
    promoteNumeric(a, b) match
      case (x: Double, y: Double) => x - y
      case (x: Long,   y: Long)   => x - y
      case _                      => toDouble(a) - toDouble(b)

  def mul(a: Any, b: Any): Any =
    promoteNumeric(a, b) match
      case (x: Double, y: Double) => x * y
      case (x: Long,   y: Long)   => x * y
      case _                      => toDouble(a) * toDouble(b)

  def div(a: Any, b: Any): Any =
    promoteNumeric(a, b) match
      case (x: Double, y: Double) => x / y
      case (x: Long,   y: Long)   => x / y
      case _                      => toDouble(a) / toDouble(b)

  def mod(a: Any, b: Any): Any =
    promoteNumeric(a, b) match
      case (x: Double, y: Double) => x % y
      case (x: Long,   y: Long)   => x % y
      case _                      => toDouble(a) % toDouble(b)

  def bitAnd(a: Any, b: Any): Any = toLong(a) & toLong(b)
  def bitOr(a: Any, b: Any): Any  = toLong(a) | toLong(b)
  def bitXor(a: Any, b: Any): Any = toLong(a) ^ toLong(b)
  def shl(a: Any, b: Any): Any    = toLong(a) << toLong(b).toInt
  def shr(a: Any, b: Any): Any    = toLong(a) >> toLong(b).toInt
  def ushr(a: Any, b: Any): Any   = toLong(a) >>> toLong(b).toInt

  def neg(a: Any): Any = a match
    case i: Int    => -i
    case l: Long   => -l
    case f: Float  => -f
    case d: Double => -d
    case i: java.lang.Integer => -i.toLong
    case _         => -toDouble(a)

  def bitNot(a: Any): Any = ~toLong(a)

  // ── Comparison ─────────────────────────────────────────────────────────────

  def eq(a: Any, b: Any): Boolean = (a, b) match
    case (null, null) => true
    case (null, _)    => false
    case (_, null)    => false
    case _            => a == b

  def lt(a: Any, b: Any): Boolean =
    promoteNumeric(a, b) match
      case (x: Double, y: Double) => x < y
      case (x: Long,   y: Long)   => x < y
      case _                      => toDouble(a) < toDouble(b)

  def gt(a: Any, b: Any): Boolean = lt(b, a)
  def le(a: Any, b: Any): Boolean = !gt(a, b)
  def ge(a: Any, b: Any): Boolean = !lt(a, b)

  // ── Boolean coercion ───────────────────────────────────────────────────────

  def toBoolean(v: Any): Boolean = v match
    case null          => false
    case b: Boolean    => b
    case b: java.lang.Boolean => b
    case i: Int        => i != 0
    case l: Long       => l != 0L
    case f: Float      => f != 0f
    case d: Double     => d != 0.0
    case s: String     => s.nonEmpty
    case _             => true

  // ── String conversion ──────────────────────────────────────────────────────

  def toStr(v: Any): String = v match
    case null   => "null"
    case s: String => s
    case b: Boolean => b.toString
    case _      => v.toString
