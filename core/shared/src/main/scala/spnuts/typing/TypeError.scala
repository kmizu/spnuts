package spnuts.typing

import spnuts.ast.SourcePos

final case class TypeError(
  msg: String,
  pos: SourcePos,
  expected: Option[StaticType] = None,
  actual: Option[StaticType] = None
) extends RuntimeException(TypeError.renderMessage(msg, pos, expected, actual))

object TypeError:
  private def renderMessage(
    msg: String,
    pos: SourcePos,
    expected: Option[StaticType],
    actual: Option[StaticType]
  ): String =
    val details = List(
      expected.map(tpe => s"expected ${tpe.displayName}"),
      actual.map(tpe => s"actual ${tpe.displayName}")
    ).flatten
    val suffix = if details.isEmpty then "" else details.mkString(" (", ", ", ")")
    s"Type error: $msg$suffix at $pos"
