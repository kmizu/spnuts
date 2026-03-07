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
      initialized = true
