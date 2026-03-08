package spnuts.repl

import spnuts.ast.SourcePos
import spnuts.interpreter.{Interpreter, RuntimeError}
import spnuts.parser.{Lexer, Parser, ParseError}
import spnuts.runtime.{Context, Operators}

/**
 * Interactive REPL for SPnuts.
 * Platform-specific subclasses provide readline / JLine support.
 */
class Repl:
  val ctx: Context = Context()
  ctx.writer.println(banner)

  private def banner: String =
    """SPnuts 2.0.0-SNAPSHOT (Scala reimplementation)
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
          case e: ParseError   => s"Parse error: ${e.message}"
          case e: RuntimeError => s"Runtime error: ${e.msg}"
          case e: Throwable    => s"Error: ${e.getMessage}"

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
