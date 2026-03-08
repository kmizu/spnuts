package spnuts.runtime

/**
 * JVM platform initialization.
 * Must be called once before using the interpreter on JVM.
 */
object JvmPlatform:
  private var initialized = false

  def init(): Unit =
    if !initialized then
      JavaInterop.install()
      PnutsPackage.initGlobals()
      installJvmBuiltins()
      initialized = true

  /** Register JVM-only built-in functions (file I/O, etc.). */
  private def installJvmBuiltins(): Unit =
    val pkg = PnutsPackage.global

    // load(path) — read and execute a .pnuts script file
    val loadFn = NativeFunc("load", 1) { (args, ctx) =>
      val path = Operators.toStr(args(0))
      val content = java.nio.file.Files.readString(java.nio.file.Paths.get(path))
      val ast = spnuts.parser.Parser.parse(content, path)
      spnuts.interpreter.Interpreter.eval(ast, ctx)
    }
    val loadGroup = PnutsGroup(Some("load"))
    loadGroup.register(loadFn)
    pkg.set("load", loadGroup)

    // readFile(path) — read file contents as a String
    val readFileFn = NativeFunc("readFile", 1) { (args, _) =>
      val path = Operators.toStr(args(0))
      java.nio.file.Files.readString(java.nio.file.Paths.get(path))
    }
    val readFileGroup = PnutsGroup(Some("readFile"))
    readFileGroup.register(readFileFn)
    pkg.set("readFile", readFileGroup)

    // writeFile(path, content) — write a String to a file
    val writeFileFn = NativeFunc("writeFile", 2) { (args, _) =>
      val path = Operators.toStr(args(0))
      val content = Operators.toStr(args(1))
      java.nio.file.Files.writeString(java.nio.file.Paths.get(path), content)
      null
    }
    val writeFileGroup = PnutsGroup(Some("writeFile"))
    writeFileGroup.register(writeFileFn)
    pkg.set("writeFile", writeFileGroup)
