package spnuts.repl

/**
 * Scala Native REPL entry point.
 * Uses simple stdin readline for now (no readline library dependency).
 */
object Main:
  def main(args: Array[String]): Unit =
    val repl = Repl()

    if args.nonEmpty then
      val src = scala.io.Source.fromFile(args(0)).mkString
      try
        val result = repl.eval(src)
        if result.nonEmpty then println(result)
      catch
        case _: QuitException => ()
    else
      var running = true
      while running do
        print("pnuts> ")
        Console.flush()
        val line = scala.io.StdIn.readLine()
        if line == null then
          running = false
        else
          try
            val result = repl.eval(line)
            if result.nonEmpty then println(result)
          catch
            case _: QuitException => running = false
            case e: Throwable     => println(s"Error: ${e.getMessage}")
