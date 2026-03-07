package spnuts.ast

/** Source position for error reporting. */
case class SourcePos(file: String, line: Int, column: Int):
  override def toString: String = s"$file:$line:$column"

object SourcePos:
  val unknown: SourcePos = SourcePos("<unknown>", 0, 0)
