package spnuts.parser

import spnuts.ast.*
import spnuts.ast.SourcePos.*
// Use TK alias to avoid collision between ast.Ident and TokenKind.Ident
private type TK = TokenKind
private val TK = TokenKind

/**
 * Hand-written PEG parser for SPnuts.
 *
 * Grammar follows Pnuts.jjt. Operator precedence (low → high):
 *  1  assignment  = *= /= %= += -= <<= >>= >>>= &= ^= |=
 *  2  ternary     ? :
 *  3  ||
 *  4  &&
 *  5  |
 *  6  ^
 *  7  &
 *  8  == !=
 *  9  instanceof
 * 10  < > <= >=
 * 11  << >> >>>
 * 12  + -
 * 13  * / %
 * 14  unary prefix  + - ~ !  ++ --
 * 15  unary postfix ++ --
 * 16  primary suffix  . :: [] ()
 */
final class Parser(tokens: IndexedSeq[Token]):
  private var cursor = 0

  // ── public entry points ────────────────────────────────────────────────────

  def parseAll(): ExprList =
    val pos    = current.pos
    skipEols()
    val exprs  = parseExprList()
    expect(TK.Eof)
    ExprList(exprs, pos)

  def parseExpr(): Expr =
    skipEols()
    val e = expression()
    skipEols()
    e

  // ── token stream helpers ───────────────────────────────────────────────────

  private def current: Token = tokens(cursor min (tokens.length - 1))
  private def peek(offset: Int = 1): Token = tokens((cursor + offset) min (tokens.length - 1))

  private def advance(): Token =
    val t = current
    if cursor < tokens.length - 1 then cursor += 1
    t

  private def check(k: TokenKind): Boolean = current.kind == k

  private def eat(k: TokenKind): Option[Token] =
    if check(k) then Some(advance()) else None

  private def expect(k: TokenKind): Token =
    if check(k) then advance()
    else throw ParseError.unexpected(k.toString, current)

  private def skipEols(): Unit =
    while check(TK.Eol) || check(TK.Semi) do advance()

  private def eatEolOrSemi(): Unit =
    eat(TK.Eol).orElse(eat(TK.Semi)); ()

  // ── Expression list ────────────────────────────────────────────────────────

  private def parseExprList(): List[Expr] =
    val exprs = collection.mutable.ListBuffer.empty[Expr]
    skipEols()
    while !check(TK.Eof) && !check(TK.RBrace) do
      exprs += expression()
      // consume statement separator
      while check(TK.Eol) || check(TK.Semi) do advance()
    exprs.toList

  // ── Expressions (precedence climbing) ─────────────────────────────────────

  private def expression(): Expr =
    multiAssignOrAssignment()

  private def multiAssignOrAssignment(): Expr =
    // Detect  a, b = expr  (multi-assign LHS).
    // Wrap in try/catch so a non-Ident after comma (e.g. a string in an arg list)
    // backtracks cleanly instead of propagating a ParseError.
    val saved = cursor
    if check(TK.Ident) && peek().kind == TK.Comma then
      try
        val targets = collection.mutable.ListBuffer.empty[Ident]
        val firstPos = current.pos
        targets += Ident(advance().image, firstPos)
        while eat(TK.Comma).isDefined do
          val p = current.pos
          targets += Ident(expect(TK.Ident).image, p)
        if check(TK.Assign) then
          advance()
          val rhs = expression()
          return MultiAssign(targets.toList, rhs, firstPos)
        cursor = saved
      catch
        case _: ParseError => cursor = saved

    assignment()

  private def assignment(): Expr =
    val lhs = ternary()
    val pos  = current.pos
    current.kind match
      case TK.Assign       => advance(); Assignment(AssignOp.Assign, lhs, expression(), pos)
      case TK.AddAssign    => advance(); Assignment(AssignOp.AddAssign, lhs, expression(), pos)
      case TK.SubAssign    => advance(); Assignment(AssignOp.SubAssign, lhs, expression(), pos)
      case TK.MulAssign    => advance(); Assignment(AssignOp.MulAssign, lhs, expression(), pos)
      case TK.DivAssign    => advance(); Assignment(AssignOp.DivAssign, lhs, expression(), pos)
      case TK.ModAssign    => advance(); Assignment(AssignOp.ModAssign, lhs, expression(), pos)
      case TK.ShlAssign    => advance(); Assignment(AssignOp.ShiftLeftAssign, lhs, expression(), pos)
      case TK.ShrAssign    => advance(); Assignment(AssignOp.ShiftRightAssign, lhs, expression(), pos)
      case TK.UshrAssign   => advance(); Assignment(AssignOp.UnsignedShiftRightAssign, lhs, expression(), pos)
      case TK.AndAssign    => advance(); Assignment(AssignOp.AndAssign, lhs, expression(), pos)
      case TK.XorAssign    => advance(); Assignment(AssignOp.XorAssign, lhs, expression(), pos)
      case TK.OrAssign     => advance(); Assignment(AssignOp.OrAssign, lhs, expression(), pos)
      case _            => lhs

  private def ternary(): Expr =
    val lhs = logicalOr()
    if check(TK.Question) then
      val pos = current.pos
      advance()
      val thenE = expression()
      expect(TK.Colon)
      val elseE = ternary()
      TernaryExpr(lhs, thenE, elseE, pos)
    else lhs

  private def rangeOp(): Expr =
    val lhs = logicalOr()
    if check(TK.DotDot) then
      val pos = current.pos; advance()
      RangeExpr(lhs, logicalOr(), pos)
    else lhs

  private def logicalOr(): Expr =
    var lhs = logicalAnd()
    while check(TK.OrOr) do
      val pos = current.pos; advance()
      lhs = BinaryExpr(BinOp.LogOr, lhs, logicalAnd(), pos)
    lhs

  private def logicalAnd(): Expr =
    var lhs = bitwiseOr()
    while check(TK.AndAnd) do
      val pos = current.pos; advance()
      lhs = BinaryExpr(BinOp.LogAnd, lhs, bitwiseOr(), pos)
    lhs

  private def bitwiseOr(): Expr =
    var lhs = bitwiseXor()
    while check(TK.Or) do
      val pos = current.pos; advance()
      lhs = BinaryExpr(BinOp.BitOr, lhs, bitwiseXor(), pos)
    lhs

  private def bitwiseXor(): Expr =
    var lhs = bitwiseAnd()
    while check(TK.Xor) do
      val pos = current.pos; advance()
      lhs = BinaryExpr(BinOp.BitXor, lhs, bitwiseAnd(), pos)
    lhs

  private def bitwiseAnd(): Expr =
    var lhs = equality()
    while check(TK.And) do
      val pos = current.pos; advance()
      lhs = BinaryExpr(BinOp.BitAnd, lhs, equality(), pos)
    lhs

  private def equality(): Expr =
    var lhs = relational()
    var continue = true
    while continue do
      current.kind match
        case TK.EqEq  => val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.Eq,    lhs, relational(), pos)
        case TK.BangEq=> val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.NotEq, lhs, relational(), pos)
        case _     => continue = false
    lhs

  private def relational(): Expr =
    var lhs = shift()
    var continue = true
    while continue do
      current.kind match
        case TK.Lt         => val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.Lt, lhs, shift(), pos)
        case TK.Gt         => val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.Gt, lhs, shift(), pos)
        case TK.Le         => val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.Le, lhs, shift(), pos)
        case TK.Ge         => val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.Ge, lhs, shift(), pos)
        case TK.Instanceof =>
          val pos = current.pos; advance()
          val name = parseClassName()
          lhs = InstanceofExpr(lhs, name, pos)
        case _ => continue = false
    lhs

  private def shift(): Expr =
    var lhs = additive()
    var continue = true
    while continue do
      current.kind match
        case TK.Shl  => val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.ShiftLeft, lhs, additive(), pos)
        case TK.Shr  => val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.ShiftRight, lhs, additive(), pos)
        case TK.Ushr => val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.UnsignedShiftRight, lhs, additive(), pos)
        case _    => continue = false
    lhs

  private def additive(): Expr =
    var lhs = multiplicative()
    var continue = true
    while continue do
      current.kind match
        case TK.Plus  => val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.Add, lhs, multiplicative(), pos)
        case TK.Minus => val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.Sub, lhs, multiplicative(), pos)
        case _     => continue = false
    lhs

  private def multiplicative(): Expr =
    var lhs = unaryPrefix()
    var continue = true
    while continue do
      current.kind match
        case TK.Star    => val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.Mul, lhs, unaryPrefix(), pos)
        case TK.Slash   => val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.Div, lhs, unaryPrefix(), pos)
        case TK.Percent => val pos = current.pos; advance(); lhs = BinaryExpr(BinOp.Mod, lhs, unaryPrefix(), pos)
        case _       => continue = false
    lhs

  private def unaryPrefix(): Expr =
    val pos = current.pos
    current.kind match
      case TK.Minus    => advance(); UnaryExpr(UnaryOp.Neg,     unaryPrefix(), pos)
      case TK.Plus     => advance(); unaryPrefix() // unary + is no-op
      case TK.Tilde    => advance(); UnaryExpr(UnaryOp.BitNot,  unaryPrefix(), pos)
      case TK.Bang     => advance(); UnaryExpr(UnaryOp.LogNot,  unaryPrefix(), pos)
      case TK.PlusPlus => advance(); UnaryExpr(UnaryOp.PreIncr, unaryPrefix(), pos)
      case TK.MinusMinus=>advance(); UnaryExpr(UnaryOp.PreDecr, unaryPrefix(), pos)
      case _        => unarySuffix()

  private def unarySuffix(): Expr =
    var base = primaryWithSuffixes()
    val pos  = current.pos
    current.kind match
      case TK.PlusPlus  => advance(); UnaryExpr(UnaryOp.PostIncr, base, pos)
      case TK.MinusMinus=> advance(); UnaryExpr(UnaryOp.PostDecr, base, pos)
      case _         => base

  // ── Primary expressions ────────────────────────────────────────────────────

  private def primaryWithSuffixes(): Expr =
    var base = primaryPrefix()
    var continue = true
    while continue do
      current.kind match
        case TK.Dot =>
          val pos = current.pos; advance()
          val name = expect(TK.Ident).image
          if check(TK.LParen) then
            val args = parseArgs()
            base = MethodCall(base, name, args, pos)
          else
            base = MemberAccess(base, name, pos)

        case TK.ColonColon =>
          val pos = current.pos; advance()
          val name = expect(TK.Ident).image
          if check(TK.LParen) then
            val args = parseArgs()
            base = StaticMethodCall(base, name, args, pos)
          else
            base = StaticMemberAccess(base, name, pos)

        case TK.LParen =>
          val pos  = current.pos
          val args = parseArgs()
          base = FuncCall(base, args, pos)

        case TK.LBracket =>
          val pos = current.pos; advance()
          val idx = expression()
          if check(TK.DotDot) then
            advance()
            val to = if check(TK.RBracket) then None else Some(expression())
            expect(TK.RBracket)
            base = RangeAccess(base, idx, to, pos)
          else
            expect(TK.RBracket)
            base = IndexAccess(base, idx, pos)

        case _ => continue = false
    base

  private def primaryPrefix(): Expr =
    val pos = current.pos
    current.kind match
      case TK.IntLit     => Token2Int(advance(), pos)
      case TK.FloatLit   => Token2Float(advance(), pos)
      case TK.CharLit    => Token2Char(advance(), pos)
      case TK.StringLit   => StringLit(advance().image, pos)
      case TK.StringStart => parseInterpolatedString(pos)
      case TK.True       => advance(); BoolLit(true, pos)
      case TK.False      => advance(); BoolLit(false, pos)
      case TK.Null       => advance(); NullLit(pos)

      case TK.Ident      =>
        val name = advance().image
        Ident(name, pos)

      case TK.ColonColon =>
        // ::name  — global reference
        advance()
        val name = expect(TK.Ident).image
        GlobalRef(name, pos)

      case TK.New        => parseNew(pos)
      case TK.Function   => parseFuncDef(pos, named = true)
      case TK.Class      => parseClassRef(pos)
      case TK.If         => parseIf(pos)
      case TK.While      => parseWhile(pos)
      case TK.Do         => parseDo(pos)
      case TK.For        => parseFor(pos)
      case TK.Foreach    => parseForeach(pos)
      case TK.Switch     => parseSwitch(pos)
      case TK.Try        => parseTry(pos)
      case TK.Return     => advance(); ReturnExpr(if isExprStart then Some(expression()) else None, pos)
      case TK.Break      => advance(); BreakExpr(if isExprStart then Some(expression()) else None, pos)
      case TK.Continue   => advance(); ContinueExpr(pos)
      case TK.Yield      => advance(); YieldExpr(if isExprStart then Some(expression()) else None, pos)
      case TK.Throw      => advance(); ThrowExpr(if isExprStart then Some(expression()) else None, pos)

      case TK.LParen     =>
        advance()
        val e = expression()
        expect(TK.RParen)
        e

      case TK.LBracket   => parseListLiteral(pos)

      case TK.LBrace     => parseBraceExpr(pos)

      case TK.Import     => parseImport(pos)
      case TK.Package    => parsePackage(pos)

      case _          =>
        throw ParseError.unexpected("expression", current)

  // ── Statement parsers ──────────────────────────────────────────────────────

  private def isExprStart: Boolean = current.kind match
    case TK.Eol | TK.Semi | TK.RBrace | TK.RParen | TK.RBracket | TK.Eof => false
    case _ => true

  private def parseInterpolatedString(pos: SourcePos): Expr =
    expect(TK.StringStart)
    val parts = collection.mutable.ListBuffer.empty[Either[String, Expr]]
    var done  = false
    while !done do
      current.kind match
        case TK.StringChunk =>
          parts += Left(advance().image)
        case TK.InterpStart =>
          advance()
          parts += Right(expression())
          expect(TK.InterpEnd)
        case TK.StringEnd =>
          advance()
          done = true
        case _ =>
          done = true
    InterpolatedString(parts.toList, pos)

  private def parseFuncDef(pos: SourcePos, named: Boolean): Expr =
    expect(TK.Function)
    val name = if named && check(TK.Ident) then Some(advance().image) else None
    expect(TK.LParen)
    val (params, varargs) = parseParamList()
    expect(TK.RParen)
    skipEols()
    val body = parseBlock()
    FuncDef(name, params, varargs, body, pos)

  private def parseParamList(): (List[String], Boolean) =
    val params  = collection.mutable.ListBuffer.empty[String]
    var varargs = false
    while !check(TK.RParen) do
      if check(TK.LBracket) && peek().kind == TK.RBracket then
        advance(); advance(); varargs = true
      else
        params += expect(TK.Ident).image
        eat(TK.Comma); ()
    (params.toList, varargs)

  private def parseBlock(): Expr =
    val pos = current.pos
    if check(TK.LBrace) then
      advance()
      skipEols()
      // Check if this is a closure:  { params -> body }
      val saved = cursor
      val maybeClosure = tryParseClosure(pos)
      if maybeClosure.isDefined then return maybeClosure.get

      val exprs = parseExprList()
      expect(TK.RBrace)
      Block(exprs, pos)
    else
      expression()

  private def tryParseClosure(pos: SourcePos): Option[FuncDef] =
    // { id [, id]* -> body }
    val saved = cursor
    try
      val params = collection.mutable.ListBuffer.empty[String]
      var varargs = false
      if check(TK.Ident) then
        params += advance().image
        while check(TK.Comma) do
          advance()
          if check(TK.LBracket) && peek().kind == TK.RBracket then
            advance(); advance(); varargs = true
          else
            params += expect(TK.Ident).image
        if check(TK.Arrow) then
          advance()
          skipEols()
          val body = parseExprList()
          expect(TK.RBrace)
          return Some(FuncDef(None, params.toList, varargs, Block(body, pos), pos))
      cursor = saved
      None
    catch
      case _: ParseError => cursor = saved; None

  private def parseIf(pos: SourcePos): Expr =
    expect(TK.If)
    expect(TK.LParen)
    val cond = expression()
    expect(TK.RParen)
    skipEols()
    val thenB = parseBlock()
    skipEols()
    val elseIfs = collection.mutable.ListBuffer.empty[(Expr, Expr)]
    var elseBranch: Option[Expr] = None
    while check(TK.Else) do
      advance()
      skipEols()
      if check(TK.If) then
        val epos = current.pos; advance()
        expect(TK.LParen)
        val ec = expression()
        expect(TK.RParen)
        skipEols()
        val eb = parseBlock()
        elseIfs += (ec -> eb)
        skipEols()
      else
        elseBranch = Some(parseBlock())
    IfExpr(cond, thenB, elseIfs.toList, elseBranch, pos)

  private def parseWhile(pos: SourcePos): Expr =
    expect(TK.While)
    expect(TK.LParen)
    val cond = expression()
    expect(TK.RParen)
    skipEols()
    val body = parseBlock()
    WhileExpr(cond, body, pos)

  private def parseDo(pos: SourcePos): Expr =
    expect(TK.Do)
    skipEols()
    val body = parseBlock()
    skipEols()
    expect(TK.While)
    expect(TK.LParen)
    val cond = expression()
    expect(TK.RParen)
    DoWhileExpr(body, cond, pos)

  private def parseFor(pos: SourcePos): Expr =
    expect(TK.For)
    expect(TK.LParen)
    // for (id : expr) or for (id, id : expr) or for (id : start..end) — ForEach
    val saved = cursor
    try
      val vars = collection.mutable.ListBuffer.empty[String]
      vars += expect(TK.Ident).image
      while check(TK.Comma) do
        advance()
        vars += expect(TK.Ident).image
      if check(TK.Colon) then
        advance()
        val iterBase = expression()
        // Allow  from..to  range syntax here
        val iter = if check(TK.DotDot) then
          val rpos = current.pos; advance()
          RangeExpr(iterBase, expression(), rpos)
        else iterBase
        expect(TK.RParen)
        skipEols()
        val body = parseBlock()
        return ForEachExpr(vars.toList, iter, body, pos)
      cursor = saved
    catch
      case _: ParseError => cursor = saved

    // C-style for
    val init = if !check(TK.Semi) then Some(expression()) else None
    expect(TK.Semi)
    val cond = if !check(TK.Semi) then Some(expression()) else None
    expect(TK.Semi)
    val upd  = if !check(TK.RParen) then
      val e = collection.mutable.ListBuffer.empty[Expr]
      e += expression()
      while check(TK.Comma) do { advance(); e += expression() }
      Some(ExprList(e.toList, current.pos))
    else None
    expect(TK.RParen)
    skipEols()
    val body = parseBlock()
    ForExpr(init, cond, upd, body, pos)

  private def parseForeach(pos: SourcePos): Expr =
    expect(TK.Foreach)
    val varName = expect(TK.Ident).image
    val iter =
      if check(TK.LParen) then
        advance()
        val e = expression()
        expect(TK.RParen)
        e
      else
        expect(TK.LBracket)
        val e = expression()
        expect(TK.RBracket)
        e
    skipEols()
    val body = parseBlock()
    ForeachExpr(varName, iter, body, pos)

  private def parseSwitch(pos: SourcePos): Expr =
    expect(TK.Switch)
    expect(TK.LParen)
    val target = expression()
    expect(TK.RParen)
    expect(TK.LBrace)
    skipEols()
    val cases = collection.mutable.ListBuffer.empty[SwitchCase]
    while !check(TK.RBrace) && !check(TK.Eof) do
      val labels = collection.mutable.ListBuffer.empty[Option[Expr]]
      while check(TK.Case) || check(TK.Default) do
        if check(TK.Case) then
          advance()
          labels += Some(expression())
          expect(TK.Colon)
        else
          advance()
          expect(TK.Colon)
          labels += None
        skipEols()
      val body = collection.mutable.ListBuffer.empty[Expr]
      while !check(TK.Case) && !check(TK.Default) && !check(TK.RBrace) && !check(TK.Eof) do
        body += expression()
        while check(TK.Eol) || check(TK.Semi) do advance()
      cases += SwitchCase(labels.toList, Block(body.toList, pos))
    expect(TK.RBrace)
    SwitchExpr(target, cases.toList, pos)

  private def parseTry(pos: SourcePos): Expr =
    expect(TK.Try)
    val body = parseBlock2()
    skipEols()
    val catches = collection.mutable.ListBuffer.empty[CatchClause]
    while check(TK.Catch) do
      val cp = current.pos; advance()
      expect(TK.LParen)
      val typeName = parseClassName()
      val varName  = expect(TK.Ident).image
      expect(TK.RParen)
      val cb = parseBlock2()
      catches += CatchClause(typeName, varName, cb, cp)
      skipEols()
    val fin = if check(TK.Finally) then
      advance()
      Some(parseBlock2())
    else None
    TryExpr(body, catches.toList, fin, pos)

  private def parseBlock2(): Expr =
    val pos = current.pos
    expect(TK.LBrace)
    skipEols()
    val exprs = parseExprList()
    expect(TK.RBrace)
    Block(exprs, pos)

  private def parseNew(pos: SourcePos): Expr =
    expect(TK.New)
    val name = parseClassName()
    if check(TK.LBracket) then
      // array: new Type[n]
      val dims = collection.mutable.ListBuffer.empty[Expr]
      while check(TK.LBracket) do
        advance()
        if !check(TK.RBracket) then dims += expression()
        expect(TK.RBracket)
      NewExpr(name, dims.toList, Nil, None, pos)
    else
      val args = parseArgs()
      val body = if check(TK.LBrace) then
        advance()
        skipEols()
        // anonymous class body — simplified for Phase 1
        while !check(TK.RBrace) && !check(TK.Eof) do
          skipEols(); advance()
        expect(TK.RBrace)
        None
      else None
      NewExpr(name, Nil, args, body, pos)

  private def parseClassRef(pos: SourcePos): Expr =
    expect(TK.Class)
    if check(TK.LParen) then
      advance()
      val e = expression()
      expect(TK.RParen)
      ClassExpr(e, pos)
    else if check(TK.Ident) then
      ClassRef(parseClassName(), pos)
    else
      throw ParseError.unexpected("class name or '('", current)

  private def parseListLiteral(pos: SourcePos): Expr =
    expect(TK.LBracket)
    val elems = collection.mutable.ListBuffer.empty[Expr]
    skipEols()
    while !check(TK.RBracket) do
      elems += expression()
      skipEols()
      eat(TK.Comma); ()
      skipEols()
    expect(TK.RBracket)
    ListExpr(elems.toList, false, pos)

  /**
   * Brace expression: could be
   *   - block: { e1; e2; ... }
   *   - map:   { k => v, ... }
   *   - list:  { e1, e2, ... }  (braced list, str="{"  in original)
   *   - closure: { params -> body }   (handled in tryParseClosure)
   */
  private def parseBraceExpr(pos: SourcePos): Expr =
    expect(TK.LBrace)
    skipEols()

    // Empty brace → empty block
    if check(TK.RBrace) then
      advance()
      return Block(Nil, pos)

    // Try closure: { params -> body }
    val saved = cursor
    val maybeClosure = tryParseClosure(pos)
    if maybeClosure.isDefined then return maybeClosure.get

    // Peek: is this a map literal?  first expr followed by FatArrow
    val savedForMap = cursor
    try
      val key = expression()
      if check(TK.FatArrow) then
        // Map literal
        advance()
        val value = expression()
        val entries = collection.mutable.ListBuffer.empty[(Expr, Expr)]
        entries += (key -> value)
        while check(TK.Comma) || check(TK.Eol) do
          eat(TK.Comma).orElse(eat(TK.Eol)); ()
          skipEols()
          if !check(TK.RBrace) then
            val k = expression()
            expect(TK.FatArrow)
            val v = expression()
            entries += (k -> v)
        skipEols()
        expect(TK.RBrace)
        return MapExpr(entries.toList, pos)

      // Not a map — treat as block or list
      cursor = savedForMap
    catch
      case _: ParseError => cursor = savedForMap

    // Block
    val exprs = parseExprList()
    expect(TK.RBrace)
    Block(exprs, pos)

  private def parseImport(pos: SourcePos): Expr =
    expect(TK.Import)
    val isStatic = eat(TK.Static).isDefined
    if check(TK.LParen) then
      advance()
      val e = expression()
      expect(TK.RParen)
      return ImportExpr(Nil, false, isStatic, Some(e), pos)
    if check(TK.Star) then
      advance()
      return ImportExpr(List("*"), wildcard = true, isStatic, None, pos)
    val parts = parseClassNameRaw()
    val wildcard = if check(TK.Dot) && peek().kind == TK.Star then
      advance(); advance(); true
    else false
    ImportExpr(parts, wildcard, isStatic, None, pos)

  private def parsePackage(pos: SourcePos): Expr =
    expect(TK.Package)
    if check(TK.LParen) then
      advance()
      val e = expression()
      expect(TK.RParen)
      return PackageExpr(Nil, Some(e), pos)
    PackageExpr(parseClassNameRaw(), None, pos)

  // ── Helpers ────────────────────────────────────────────────────────────────

  private def parseArgs(): List[Expr] =
    expect(TK.LParen)
    val args = collection.mutable.ListBuffer.empty[Expr]
    skipEols()
    while !check(TK.RParen) do
      args += expression()
      skipEols()
      eat(TK.Comma); ()
      skipEols()
    expect(TK.RParen)
    args.toList

  /** Parse a qualified class name: Foo.Bar.Baz */
  private def parseClassName(): List[String] =
    val parts = collection.mutable.ListBuffer.empty[String]
    parts += expect(TK.Ident).image
    while check(TK.Dot) && peek().kind == TK.Ident do
      advance()
      parts += advance().image
    parts.toList

  private def parseClassNameRaw(): List[String] =
    val parts = collection.mutable.ListBuffer.empty[String]
    parts += expect(TK.Ident).image
    while check(TK.Dot) && (peek().kind == TK.Ident || peek().kind == TK.Star) do
      advance()
      parts += (if check(TK.Star) then { advance(); "*" } else advance().image)
    parts.toList

  // ── Token to AST conversions ───────────────────────────────────────────────

  private def Token2Int(tok: Token, pos: SourcePos): IntLit =
    val raw = tok.image
    val (numStr, suffix) =
      if raw.last.isLetter then (raw.init, Some(raw.last)) else (raw, None)
    val n: Any =
      if numStr.startsWith("0x") || numStr.startsWith("0X") then
        java.lang.Long.parseLong(numStr.drop(2), 16)
      else if numStr.startsWith("#") then
        java.lang.Long.parseLong(numStr.drop(1), 16)
      else
        java.lang.Long.parseLong(numStr)
    IntLit(n, raw, pos)

  private def Token2Float(tok: Token, pos: SourcePos): FloatLit =
    val raw = tok.image
    FloatLit(raw.toDouble, raw, pos)

  private def Token2Char(tok: Token, pos: SourcePos): CharLit =
    val raw = tok.image // e.g. "'a'" or "'\n'"
    val c = if raw.length >= 2 then raw(1) else raw(0)
    CharLit(c, pos)

// ── companion object: convenient factory ──────────────────────────────────────

object Parser:
  def parse(source: String, file: String = "<input>"): ExprList =
    val tokens = Lexer(source, file).tokenize()
    Parser(tokens).parseAll()

  def parseExpr(source: String, file: String = "<input>"): Expr =
    val tokens = Lexer(source, file).tokenize()
    Parser(tokens).parseExpr()
