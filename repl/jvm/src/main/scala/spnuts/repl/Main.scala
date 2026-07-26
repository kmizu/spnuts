package spnuts.repl

import spnuts.runtime.JvmPlatform
import org.jline.reader.{LineReaderBuilder, EndOfFileException, UserInterruptException}
import org.jline.terminal.TerminalBuilder

/**
 * JVM REPL entry point with JLine3 for readline support.
 */
object Main:
  def main(args: Array[String]): Unit =
    JvmPlatform.init()
    val repl = Repl()

    if args.nonEmpty then
      // Script mode: evaluate file
      try
        val src = scala.io.Source.fromFile(args(0)).mkString
        val result = repl.eval(src)
        if result.nonEmpty then println(result)
      catch
        case e: java.io.FileNotFoundException => println(s"Error: ${e.getMessage}")
    else
      // Interactive mode
      val terminal = TerminalBuilder.terminal()
      val reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .variable(org.jline.reader.LineReader.HISTORY_FILE, ".spnuts_history")
        .build()

      var running = true
      while running do
        try
          val line = reader.readLine(repl.prompt)
          if line != null then
            repl.step(line) match
              case StepResult.Continue    => ()
              case StepResult.Output(txt) => if txt.nonEmpty then println(txt)
              case StepResult.Quit        => running = false
        catch
          case _: EndOfFileException     => running = false
          case _: UserInterruptException => running = false
          case e: Throwable              => println(s"Error: ${e.getMessage}")

      terminal.close()
