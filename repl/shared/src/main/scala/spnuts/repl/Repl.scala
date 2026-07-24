package spnuts.repl

import spnuts.ast.SourcePos
import spnuts.interpreter.{Interpreter, RuntimeError}
import spnuts.parser.{Lexer, Parser, ParseError}
import spnuts.runtime.{Context, Operators}

/**
 * Interactive REPL for SPnuts.
 * Platform-specific subclasses provide readline / JLine support.
 */
class Repl(val ctx: Context = Context()):
  ctx.writer.println(banner)

  private def banner: String =
    """SPnuts 0.1-SNAPSHOT (Scala reimplementation)
      |Thanks to Tomatsu-san for the original Pnuts.
      |Type :quit to exit, :help for commands.""".stripMargin

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
