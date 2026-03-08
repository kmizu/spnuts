package spnuts.parser

import spnuts.ast.SourcePos
import TokenKind.*
import scala.collection.mutable.ArrayBuffer

/**
 * Hand-written lexer for SPnuts.
 *
 * Handles two modes:
 *  - DEFAULT: normal code
 *  - STRING:  inside a "..." string (for \(expr) interpolation)
 *
 * EOL is significant as a statement separator (like Scala/Python).
 */
final class Lexer(source: String, file: String = "<input>"):
  private val buf     = source
  private val len     = source.length
  private var pos     = 0
  private var line    = 1
  private var col     = 1

  // Interpolation nesting: pushed when entering \(, popped on )
  private val interpDepth = ArrayBuffer.empty[Int]

  // Pending token queue — used to emit multiple tokens from a single lex action
  // (e.g. string interpolation produces StringStart, StringChunk, InterpStart, ...)
  private val pending = collection.mutable.Queue.empty[Token]

  // ── public API ─────────────────────────────────────────────────────────────

  /** Tokenize the entire source. */
  def tokenize(): IndexedSeq[Token] =
    val tokens = ArrayBuffer.empty[Token]
    var last   = TokenKind.Eol // pretend we started after a newline

    while pos < len || pending.nonEmpty do
      if pending.isEmpty then skipWhitespaceAndComments()
      if pos < len || pending.nonEmpty then
        val tok = nextToken()
        // Collapse multiple EOLs / suppress EOL after certain tokens
        tok.kind match
          case Eol if last == Eol || last == Semi || last == LBrace ||
                      last == Comma || last == Arrow || last == FatArrow ||
                      last == LParen || last == LBracket => () // skip
          case _ =>
            tokens += tok
            last = tok.kind

    // Ensure final EOF
    val eofPos = currentPos()
    if tokens.isEmpty || tokens.last.kind != Eof then
      tokens += Token(Eof, "", eofPos)
    tokens.toIndexedSeq

  // ── private helpers ────────────────────────────────────────────────────────

  private def currentPos(): SourcePos = SourcePos(file, line, col)

  private def peek(offset: Int = 0): Char =
    if pos + offset < len then buf(pos + offset) else '\u0000'

  private def advance(): Char =
    val c = buf(pos)
    pos += 1
    if c == '\n' then { line += 1; col = 1 } else col += 1
    c

  private def skipWhitespaceAndComments(): Unit =
    var continue = true
    while continue && pos < len do
      peek() match
        case ' ' | '\t' | '\f' | '\r' => advance(); ()
        case '\n' => continue = false // EOL is significant, handled in nextToken
        case '/' if peek(1) == '/' =>
          while pos < len && peek() != '\n' do advance()
        case '/' if peek(1) == '*' =>
          advance(); advance()
          while pos < len && !(peek() == '*' && peek(1) == '/') do advance()
          if pos < len then { advance(); advance() }
        case _ => continue = false

  private def nextToken(): Token =
    if pending.nonEmpty then return pending.dequeue()
    val startPos = currentPos()

    peek() match
      // EOL (significant)
      case '\n' =>
        advance()
        Token(Eol, "\n", startPos)

      // Shebang at very start
      case '#' if peek(1) == '!' && startPos.line == 1 && startPos.column == 1 =>
        while pos < len && peek() != '\n' do advance()
        nextToken() // skip shebang, continue

      // String literals
      case '"' =>
        advance()
        lexString(startPos)

      case '`' =>
        advance()
        lexRawString(startPos)

      case '\'' =>
        advance()
        lexChar(startPos)

      // Numbers
      case '#' =>
        advance()
        lexHexAfterHash(startPos)

      case '0' if (peek(1) == 'x' || peek(1) == 'X') =>
        advance(); advance()
        lexHex(startPos)

      case c if c.isDigit =>
        lexNumber(startPos)

      // Identifier or keyword
      case c if isIdentStart(c) =>
        lexIdent(startPos)

      // Operators and punctuation
      case '=' => advance(); if peek() == '=' then { advance(); Token(EqEq, "==", startPos) }
                              else if peek() == '>' then { advance(); Token(FatArrow, "=>", startPos) }
                              else Token(Assign, "=", startPos)
      case '!' => advance(); if peek() == '=' then { advance(); Token(BangEq, "!=", startPos) }
                              else Token(Bang, "!", startPos)
      case '<' => advance()
                  if peek() == '<' then { advance()
                    if peek() == '=' then { advance(); Token(ShlAssign, "<<=", startPos) }
                    else Token(Shl, "<<", startPos) }
                  else if peek() == '=' then { advance(); Token(Le, "<=", startPos) }
                  else Token(Lt, "<", startPos)
      case '>' => advance()
                  if peek() == '>' then
                    advance()
                    if peek() == '>' then
                      advance()
                      if peek() == '=' then { advance(); Token(UshrAssign, ">>>=", startPos) }
                      else Token(Ushr, ">>>", startPos)
                    else if peek() == '=' then { advance(); Token(ShrAssign, ">>=", startPos) }
                    else Token(Shr, ">>", startPos)
                  else if peek() == '=' then { advance(); Token(Ge, ">=", startPos) }
                  else Token(Gt, ">", startPos)
      case '&' => advance()
                  if peek() == '&' then { advance(); Token(AndAnd, "&&", startPos) }
                  else if peek() == '=' then { advance(); Token(AndAssign, "&=", startPos) }
                  else Token(And, "&", startPos)
      case '|' => advance()
                  if peek() == '|' then { advance(); Token(OrOr, "||", startPos) }
                  else if peek() == '=' then { advance(); Token(OrAssign, "|=", startPos) }
                  else Token(Or, "|", startPos)
      case '^' => advance()
                  if peek() == '=' then { advance(); Token(XorAssign, "^=", startPos) }
                  else Token(Xor, "^", startPos)
      case '+' => advance()
                  if peek() == '+' then { advance(); Token(PlusPlus, "++", startPos) }
                  else if peek() == '=' then { advance(); Token(AddAssign, "+=", startPos) }
                  else Token(Plus, "+", startPos)
      case '-' => advance()
                  if peek() == '-' then { advance(); Token(MinusMinus, "--", startPos) }
                  else if peek() == '=' then { advance(); Token(SubAssign, "-=", startPos) }
                  else if peek() == '>' then { advance(); Token(Arrow, "->", startPos) }
                  else Token(Minus, "-", startPos)
      case '*' => advance()
                  if peek() == '=' then { advance(); Token(MulAssign, "*=", startPos) }
                  else Token(Star, "*", startPos)
      case '/' => advance()
                  if peek() == '=' then { advance(); Token(DivAssign, "/=", startPos) }
                  else Token(Slash, "/", startPos)
      case '%' => advance()
                  if peek() == '=' then { advance(); Token(ModAssign, "%=", startPos) }
                  else Token(Percent, "%", startPos)
      case '~' => advance(); Token(Tilde, "~", startPos)
      case '?' => advance(); Token(Question, "?", startPos)
      case ':' => advance()
                  if peek() == ':' then { advance(); Token(ColonColon, "::", startPos) }
                  else Token(Colon, ":", startPos)
      case '.' => advance()
                  if peek() == '.' then { advance(); Token(DotDot, "..", startPos) }
                  else Token(Dot, ".", startPos)
      case '(' => advance()
                  // Track paren depth inside \(expr) so that method calls like
                  // \(obj.method()) don't prematurely close the interpolation.
                  if interpDepth.nonEmpty then
                    interpDepth(interpDepth.length - 1) += 1
                  Token(LParen, "(", startPos)
      case ')' => advance()
                  // If we're inside \(expr), this ends the interpolation
                  if interpDepth.nonEmpty then
                    val depth = interpDepth.last
                    if depth == 0 then
                      interpDepth.dropRightInPlace(1)
                      // Emit InterpEnd and queue the remaining string content
                      lexStringContinuationIntoQueue(startPos)
                      Token(InterpEnd, ")", startPos)
                    else
                      interpDepth(interpDepth.length - 1) = depth - 1
                      Token(RParen, ")", startPos)
                  else
                    Token(RParen, ")", startPos)
      case '{' => advance()
                  if interpDepth.nonEmpty then
                    interpDepth(interpDepth.length - 1) += 1
                  Token(LBrace, "{", startPos)
      case '}' => advance()
                  if interpDepth.nonEmpty then
                    val depth = interpDepth.last
                    if depth > 0 then interpDepth(interpDepth.length - 1) = depth - 1
                  Token(RBrace, "}", startPos)
      case '[' => advance(); Token(LBracket, "[", startPos)
      case ']' => advance(); Token(RBracket, "]", startPos)
      case ';' => advance(); Token(Semi, ";", startPos)
      case ',' => advance(); Token(Comma, ",", startPos)

      case c =>
        advance()
        Token(Error, c.toString, startPos)

  // ── Lexing helpers ─────────────────────────────────────────────────────────

  private def isIdentStart(c: Char): Boolean =
    c == '$' || c == '_' || c.isLetter || c >= '\u0080'

  private def isIdentPart(c: Char): Boolean =
    isIdentStart(c) || c.isDigit

  private def lexIdent(startPos: SourcePos): Token =
    val sb = new StringBuilder
    while pos < len && isIdentPart(peek()) do sb += advance()
    Token.identOrKeyword(sb.toString, startPos)

  private def lexNumber(startPos: SourcePos): Token =
    val sb = new StringBuilder
    // Integer part
    while pos < len && peek().isDigit do sb += advance()
    // Optional fraction
    if pos < len && peek() == '.' && peek(1) != '.' && peek(1).isDigit then
      sb += advance() // '.'
      while pos < len && peek().isDigit do sb += advance()
      // Optional exponent
      if pos < len && (peek() == 'e' || peek() == 'E') then
        sb += advance()
        if pos < len && (peek() == '+' || peek() == '-') then sb += advance()
        while pos < len && peek().isDigit do sb += advance()
      Token(FloatLit, sb.toString, startPos)
    else if pos < len && (peek() == 'e' || peek() == 'E') &&
            (peek(1).isDigit || peek(1) == '+' || peek(1) == '-') then
      sb += advance()
      if pos < len && (peek() == '+' || peek() == '-') then sb += advance()
      while pos < len && peek().isDigit do sb += advance()
      Token(FloatLit, sb.toString, startPos)
    else
      // Optional suffix (L, f, d, etc.)
      if pos < len && isIdentStart(peek()) then sb += advance()
      Token(IntLit, sb.toString, startPos)

  private def lexHex(startPos: SourcePos): Token =
    val sb = new StringBuilder("0x")
    while pos < len && (peek().isDigit || ('a' to 'f').contains(peek().toLower)) do
      sb += advance()
    if pos < len && isIdentStart(peek()) then sb += advance()
    Token(IntLit, sb.toString, startPos)

  private def lexHexAfterHash(startPos: SourcePos): Token =
    val sb = new StringBuilder("#")
    while pos < len && (peek().isDigit || ('a' to 'f').contains(peek().toLower)) do
      sb += advance()
    Token(IntLit, sb.toString, startPos)

  private def lexChar(startPos: SourcePos): Token =
    val c = if peek() == '\\' then { advance(); escapeChar() } else advance()
    if pos < len && peek() == '\'' then advance()
    Token(CharLit, s"'$c'", startPos)

  private def escapeChar(): Char =
    advance() match
      case 'n'  => '\n'
      case 't'  => '\t'
      case 'b'  => '\b'
      case 'r'  => '\r'
      case 'f'  => '\f'
      case '0'  => '\u0000'
      case '\\' => '\\'
      case '\'' => '\''
      case '"'  => '"'
      case 'u'  =>
        val hex = (0 until 4).map(_ => advance()).mkString
        Integer.parseInt(hex, 16).toChar
      case c => c

  /**
   * Lex a "..." string. If it contains \( interpolation, returns StringStart
   * and queues StringChunk/InterpStart tokens. Otherwise returns a plain StringLit.
   */
  private def lexString(startPos: SourcePos): Token =
    val sb = new StringBuilder
    while pos < len && peek() != '"' && peek() != '\n' do
      if peek() == '\\' then
        advance()
        if peek() == '(' then
          // String interpolation found
          advance()
          interpDepth += 0
          if sb.nonEmpty then pending.enqueue(Token(StringChunk, sb.toString, startPos))
          pending.enqueue(Token(InterpStart, "\\(", startPos))
          return Token(StringStart, "\"", startPos)
        else
          sb += escapeChar()
      else
        sb += advance()
    // No interpolation — plain string literal
    if pos < len && peek() == '"' then advance() // consume closing "
    Token(StringLit, sb.toString, startPos)

  /** Lex a raw backtick string. */
  private def lexRawString(startPos: SourcePos): Token =
    val sb = new StringBuilder
    while pos < len && peek() != '`' do sb += advance()
    if pos < len then advance()
    Token(StringLit, sb.toString, startPos)

  /**
   * After an InterpEnd, continue collecting the rest of the interpolated string.
   * Enqueues StringChunk/InterpStart/StringEnd into pending.
   */
  private def lexStringContinuationIntoQueue(startPos: SourcePos): Unit =
    val sb = new StringBuilder
    while pos < len && peek() != '"' && peek() != '\n' do
      if peek() == '\\' then
        advance()
        if peek() == '(' then
          advance()
          interpDepth += 0
          if sb.nonEmpty then pending.enqueue(Token(StringChunk, sb.toString, startPos))
          pending.enqueue(Token(InterpStart, "\\(", startPos))
          return
        else
          sb += escapeChar()
      else
        sb += advance()
    // Reached closing " (or end of line)
    if pos < len && peek() == '"' then advance()
    if sb.nonEmpty then pending.enqueue(Token(StringChunk, sb.toString, startPos))
    pending.enqueue(Token(StringEnd, "\"", startPos))
