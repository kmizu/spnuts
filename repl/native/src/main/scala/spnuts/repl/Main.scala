package spnuts.repl

/**
 * Scala Native REPL entry point.
 * Uses simple stdin readline for now (no readline library dependency).
 */
object Main:
  def main(args: Array[String]): Unit =
    spnuts.runtime.PnutsPackage.initGlobals()
    val repl = Repl()

    if args.nonEmpty then
      try
        val src = scala.io.Source.fromFile(args(0)).mkString
        val result = repl.eval(src)
        if result.nonEmpty then println(result)
      catch
        case e: java.io.FileNotFoundException => println(s"Error: ${e.getMessage}")
    else
      var running = true
      while running do
        print(repl.prompt)
        Console.flush()
        val line = scala.io.StdIn.readLine()
        if line == null then
          running = false
        else
          try
            repl.step(line) match
              case StepResult.Continue    => ()
              case StepResult.Output(txt) => if txt.nonEmpty then println(txt)
              case StepResult.Quit        => running = false
          catch
            case e: Throwable => println(s"Error: ${e.getMessage}")
