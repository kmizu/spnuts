package spnuts.parser

import spnuts.ast.SourcePos

/** Parse error with source position. */
case class ParseError(message: String, pos: SourcePos) extends Exception(s"$pos: $message")

object ParseError:
  def unexpected(expected: String, got: Token): ParseError =
    ParseError(s"Expected $expected but got ${got.kind}('${got.image}')", got.pos)
