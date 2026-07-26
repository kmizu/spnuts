package spnuts.typing

import spnuts.ast.SourcePos

final case class TypeError(
  msg: String,
  pos: SourcePos,
  expected: Option[StaticType] = None,
  actual: Option[StaticType] = None
) extends RuntimeException(s"$msg at $pos")
