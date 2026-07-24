package spnuts.repl

import spnuts.ast.SourcePos
import spnuts.interpreter.{Interpreter, RuntimeError}
import spnuts.parser.{Lexer, Parser, ParseError}
import spnuts.runtime.{Context, Operators}

/** Result of feeding one line of interactive input to a Repl. */
enum StepResult:
  case Continue
  case Output(text: String)
  case Quit

/**
 * Interactive REPL for SPnuts.
 * Platform-specific subclasses provide readline / JLine support.
 */
class Repl(val ctx: Context = Context()):
  ctx.writer.println(banner)

  private var buffer: String = ""

  private def banner: String =
    """SPnuts 0.1-SNAPSHOT (Scala reimplementation)
      |Thanks to Tomatsu-san for the original Pnuts.
      |Type :quit to exit, :help for commands.""".stripMargin

  /** Prompt to show for the next line of input. */
  def prompt: String = if buffer.isEmpty then "pnuts> " else "..... "

  /**
   * Feed one line of interactive input. Commands (`:quit`, `:help`, ...) are
   * only recognized when not in the middle of a multi-line statement.
   * Incomplete statements are buffered across calls until a full statement
   * can be parsed, at which point it's evaluated via `eval`.
   */
  def step(line: String): StepResult =
    if buffer.isEmpty then
      line.trim match
        case ":quit" | ":exit" | ":q" => return StepResult.Quit
        case ":help"                  => return StepResult.Output(helpText)
        case cmd if cmd.startsWith(":load ") =>
          return StepResult.Output(loadFile(cmd.stripPrefix(":load ").trim))
        case ""                       => return StepResult.Output("")
        case _                        => ()

    val candidate = if buffer.isEmpty then line else buffer + "\n" + line
    if isIncomplete(candidate) then
      buffer = candidate
      StepResult.Continue
    else
      buffer = ""
      StepResult.Output(eval(candidate))

  /** True if `code` fails to parse only because it ran out of input. */
  private def isIncomplete(code: String): Boolean =
    try
      Parser.parse(code, "<repl>")
      false
    catch
      case e: ParseError => e.unexpectedEof
      case _: Throwable  => false

  private def loadFile(path: String): String =
    try
      val content = scala.io.Source.fromFile(path).mkString
      eval(content)
    catch
      case e: Throwable => s"Error loading '$path': ${e.getMessage}"

  def eval(line: String): String =
    if line.isBlank then return ""
    line.trim match
      case ":quit" | ":exit" | ":q" => throw QuitException()
      case ":help"                   => helpText
      case code =>
        try
          val expr   = Parser.parse(code, "<repl>")
          val result = Interpreter.eval(expr, ctx)
          if result == null then ""
          else result match
            case _: spnuts.runtime.PnutsGroup => "" // suppress function def display
            case _                            => formatResult(result)
        catch
          case e: ParseError   => formatError("Parse error", e.pos, e.message, code)
          case e: RuntimeError => formatError("Runtime error", e.pos, e.msg, code)
          case e: Throwable    => s"Error: ${e.getMessage}"

  private def formatError(kind: String, pos: SourcePos, msg: String, source: String): String =
    val header = s"$kind at $pos: $msg"
    val lines  = source.split("\n", -1)
    if pos.line >= 1 && pos.line <= lines.length then
      val srcLine = lines(pos.line - 1)
      val caret   = " " * math.max(0, pos.column - 1) + "^"
      s"$header\n  $srcLine\n  $caret"
    else header

  private def formatResult(v: Any): String = v match
    case arr: Array[?] => s"[${arr.map(formatResult).mkString(", ")}]"
    case m: java.util.Map[?, ?] =>
      val entries = m.entrySet().toArray.map { e =>
        val entry = e.asInstanceOf[java.util.Map.Entry[?, ?]]
        s"${formatResult(entry.getKey)} => ${formatResult(entry.getValue)}"
      }
      s"{${entries.mkString(", ")}}"
    case s: String => s""""$s""""
    case null      => "null"
    case v         => v.toString

  private def helpText: String =
    """:help  — this message
      |:quit  — exit REPL
      |Any Pnuts expression is evaluated and the result printed.""".stripMargin

class QuitException extends Exception("quit")
