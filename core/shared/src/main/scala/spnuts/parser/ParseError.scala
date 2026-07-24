package spnuts.parser

import spnuts.ast.SourcePos

/**
 * Parse error with source position.
 *
 * @param unexpectedEof true when parsing failed only because input ran out
 *                       (e.g. an unclosed `{`), as opposed to a genuine
 *                       syntax error. Used by the REPL to decide whether to
 *                       keep reading more lines instead of reporting.
 */
case class ParseError(message: String, pos: SourcePos, unexpectedEof: Boolean = false)
  extends Exception(s"$pos: $message")

object ParseError:
  def unexpected(expected: String, got: Token): ParseError =
    ParseError(
      s"Expected $expected but got ${got.kind}('${got.image}')",
      got.pos,
      unexpectedEof = got.kind == TokenKind.Eof,
    )
