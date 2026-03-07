package spnuts.runtime

/**
 * An instance of a record type. Supports field access via `.fieldName`.
 * Getter-style `.getName()` is handled at the interpreter level.
 */
case class PnutsRecordInstance(typeName: String, fields: Map[String, Any]):
  def get(name: String): Option[Any] = fields.get(name)

  override def toString: String =
    val pairs = fields.map((k, v) => s"$k=$v").mkString(", ")
    s"$typeName($pairs)"

/**
 * Factory for creating record instances. Stored in the package as a NativeFunc
 * so that `Person("Alice", 30)` works like a function call.
 */
object PnutsRecord:
  def makeFactory(name: String, fieldNames: List[String]): NativeFunc =
    val arity = fieldNames.length
    NativeFunc(name, arity) { (args, _) =>
      if args.length != arity then
        throw new RuntimeException(
          s"$name: expected $arity arguments, got ${args.length}")
      val fields = fieldNames.zip(args).toMap
      PnutsRecordInstance(name, fields)
    }
