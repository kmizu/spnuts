package spnuts.parser

import spnuts.ast.SourcePos

/** Token kind enum — mirrors Pnuts.jjt token types */
enum TokenKind:
  // Literals
  case IntLit, FloatLit, CharLit
  case StringLit       // plain "..."  or  `...`
  case StringStart     // opening " of interpolated string
  case StringEnd       // closing " of interpolated string
  case StringChunk     // literal chunk inside interpolated string
  case InterpStart     // \(  inside interpolated string
  case InterpEnd       // )   ending interpolation

  // Keywords
  case True, False, Null
  case If, Else, While, Do, For, Foreach, Switch, Case, Default
  case Instanceof, Break, Continue, Return, Yield
  case Function, Class, Record, Try, Catch, Throw, Finally
  case Import, Package, Static, New, Extends, Implements

  // Identifiers
  case Ident

  // Operators
  case Assign                                // =
  case MulAssign, DivAssign, ModAssign       // *= /= %=
  case AddAssign, SubAssign                  // += -=
  case ShlAssign, ShrAssign, UshrAssign      // <<= >>= >>>=
  case AndAssign, XorAssign, OrAssign        // &= ^= |=

  case Question, Colon                       // ? :
  case OrOr, AndAnd                          // || &&
  case Or, Xor, And                          // | ^ &
  case EqEq, BangEq                          // == !=
  case Lt, Gt, Le, Ge                        // < > <= >=
  case Shl, Shr, Ushr                        // << >> >>>
  case Plus, Minus, Star, Slash, Percent     // + - * / %
  case Tilde, Bang                           // ~ !
  case PlusPlus, MinusMinus                  // ++ --
  case Dot                                   // .
  case ColonColon                            // ::
  case DotDot                                // ..
  case Arrow                                 // ->
  case FatArrow                              // =>

  // Delimiters
  case LParen, RParen                        // ( )
  case LBrace, RBrace                        // { }
  case LBracket, RBracket                    // [ ]
  case Semi                                  // ;
  case Comma                                 // ,
  case Backtick                              // ` (raw string delimiter)

  // Special
  case Eol                                   // significant newline
  case Eof
  case Shebang                               // #! line
  case Error                                 // unrecognized character

case class Token(kind: TokenKind, image: String, pos: SourcePos):
  override def toString: String = s"$kind(${image.take(20)})"

object Token:
  private val keywords: Map[String, TokenKind] = Map(
    "true"        -> TokenKind.True,
    "false"       -> TokenKind.False,
    "null"        -> TokenKind.Null,
    "if"          -> TokenKind.If,
    "else"        -> TokenKind.Else,
    "while"       -> TokenKind.While,
    "do"          -> TokenKind.Do,
    "for"         -> TokenKind.For,
    "foreach"     -> TokenKind.Foreach,
    "switch"      -> TokenKind.Switch,
    "case"        -> TokenKind.Case,
    "default"     -> TokenKind.Default,
    "instanceof"  -> TokenKind.Instanceof,
    "break"       -> TokenKind.Break,
    "continue"    -> TokenKind.Continue,
    "return"      -> TokenKind.Return,
    "yield"       -> TokenKind.Yield,
    "function"    -> TokenKind.Function,
    "class"       -> TokenKind.Class,
    "record"      -> TokenKind.Record,
    "try"         -> TokenKind.Try,
    "catch"       -> TokenKind.Catch,
    "throw"       -> TokenKind.Throw,
    "finally"     -> TokenKind.Finally,
    "import"      -> TokenKind.Import,
    "package"     -> TokenKind.Package,
    "static"      -> TokenKind.Static,
    "new"         -> TokenKind.New,
    "extends"     -> TokenKind.Extends,
    "implements"  -> TokenKind.Implements,
  )

  def identOrKeyword(image: String, pos: SourcePos): Token =
    Token(keywords.getOrElse(image, TokenKind.Ident), image, pos)
