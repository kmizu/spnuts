package spnuts.interpreter

import spnuts.ast.*
import spnuts.runtime.{*, given}

/**
 * Tree-walking interpreter for SPnuts.
 *
 * Each AST node type is handled by a pattern match arm in `eval`.
 * Control flow uses exceptions (ReturnException / BreakException / ContinueException),
 * mirroring the original PnutsInterpreter.java.
 */
object Interpreter:

  /**
   * Evaluate an expression in the given context.
   * Returns the result value (null for void statements).
   */
  def eval(expr: Expr, ctx: Context): Any =
    // Wire interpreter callback once so built-in native functions can call user functions
    if ctx.callFn == null then
      ctx.callFn = (f, args, c, pos) => callValue(f, args, c, pos)
    evalInner(expr, ctx)

  private def evalInner(expr: Expr, ctx: Context): Any = expr match

    // ── Literals ──────────────────────────────────────────────────────────────

    case IntLit(v, _, _)   => v
    case FloatLit(v, _, _) => v
    case CharLit(v, _)     => v
    case StringLit(v, _)   => v
    case BoolLit(v, _)     => v
    case NullLit(_)        => null

    case InterpolatedString(parts, _) =>
      val sb = new StringBuilder
      for part <- parts do part match
        case Left(s)  => sb ++= s
        case Right(e) => sb ++= Operators.toStr(eval(e, ctx))
      sb.toString

    // ── Identifiers ───────────────────────────────────────────────────────────

    case Ident(name, pos) =>
      try ctx.getValue(name)
      catch case e: RuntimeException =>
        throw RuntimeError(s"Undefined variable: '$name'", pos, e)

    case GlobalRef(name, pos) =>
      PnutsPackage.global.lookup(name)
        .map(_.value)
        .getOrElse(throw RuntimeError(s"Undefined global: '::$name'", pos))

    // ── Operators ─────────────────────────────────────────────────────────────

    case BinaryExpr(op, lhs, rhs, pos) =>
      import BinOp.*
      op match
        case Add    => Operators.add(eval(lhs, ctx), eval(rhs, ctx))
        case Sub    => Operators.sub(eval(lhs, ctx), eval(rhs, ctx))
        case Mul    => Operators.mul(eval(lhs, ctx), eval(rhs, ctx))
        case Div    => Operators.div(eval(lhs, ctx), eval(rhs, ctx))
        case Mod    => Operators.mod(eval(lhs, ctx), eval(rhs, ctx))
        case BitAnd => Operators.bitAnd(eval(lhs, ctx), eval(rhs, ctx))
        case BitOr  => Operators.bitOr(eval(lhs, ctx), eval(rhs, ctx))
        case BitXor => Operators.bitXor(eval(lhs, ctx), eval(rhs, ctx))
        case ShiftLeft => Operators.shl(eval(lhs, ctx), eval(rhs, ctx))
        case ShiftRight => Operators.shr(eval(lhs, ctx), eval(rhs, ctx))
        case UnsignedShiftRight => Operators.ushr(eval(lhs, ctx), eval(rhs, ctx))
        case Eq    => Operators.eq(eval(lhs, ctx), eval(rhs, ctx))
        case NotEq => !Operators.eq(eval(lhs, ctx), eval(rhs, ctx))
        case Lt    => Operators.lt(eval(lhs, ctx), eval(rhs, ctx))
        case Gt    => Operators.gt(eval(lhs, ctx), eval(rhs, ctx))
        case Le    => Operators.le(eval(lhs, ctx), eval(rhs, ctx))
        case Ge    => Operators.ge(eval(lhs, ctx), eval(rhs, ctx))
        case LogAnd =>
          val l = eval(lhs, ctx)
          if !Operators.toBoolean(l) then false else Operators.toBoolean(eval(rhs, ctx))
        case LogOr =>
          val l = eval(lhs, ctx)
          if Operators.toBoolean(l) then true else Operators.toBoolean(eval(rhs, ctx))

    case UnaryExpr(op, operand, pos) =>
      import UnaryOp.*
      op match
        case Neg     => Operators.neg(eval(operand, ctx))
        case BitNot  => Operators.bitNot(eval(operand, ctx))
        case LogNot  => !Operators.toBoolean(eval(operand, ctx))
        case PreIncr => doIncr(operand, ctx, delta = 1L, returnOld = false)
        case PreDecr => doIncr(operand, ctx, delta = -1L, returnOld = false)
        case PostIncr=> doIncr(operand, ctx, delta = 1L, returnOld = true)
        case PostDecr=> doIncr(operand, ctx, delta = -1L, returnOld = true)

    case TernaryExpr(cond, thenE, elseE, _) =>
      if Operators.toBoolean(eval(cond, ctx)) then eval(thenE, ctx)
      else eval(elseE, ctx)

    case InstanceofExpr(expr, typeName, pos) =>
      val obj = eval(expr, ctx)
      val cls = resolveClass(typeName, ctx, pos)
      cls.isInstance(obj)

    // ── Assignment ────────────────────────────────────────────────────────────

    case Assignment(op, lhs, rhs, pos) =>
      val value = computeAssign(op, lhs, rhs, ctx, pos)
      assignTo(lhs, value, ctx, pos)
      value

    case MultiAssign(targets, rhs, pos) =>
      val value = eval(rhs, ctx)
      value match
        case arr: Array[?] =>
          for (t, i) <- targets.zipWithIndex do
            ctx.setValue(t.name, if i < arr.length then arr(i) else null)
        case _ =>
          if targets.nonEmpty then ctx.setValue(targets.head.name, value)
          for t <- targets.tail do ctx.setValue(t.name, null)
      value

    // ── Collections ───────────────────────────────────────────────────────────

    case ListExpr(elements, _, _) =>
      val arr = new Array[Any](elements.size)
      for (e, i) <- elements.zipWithIndex do arr(i) = eval(e, ctx)
      arr

    case MapExpr(entries, _) =>
      val m = new java.util.LinkedHashMap[Any, Any]()
      for (k, v) <- entries do m.put(eval(k, ctx), eval(v, ctx))
      m

    // ── Block / ExprList ──────────────────────────────────────────────────────

    case Block(exprs, _) =>
      if exprs.isEmpty then null
      else exprs.foldLeft[Any](null)((_, e) => eval(e, ctx))

    case ExprList(exprs, _) =>
      if exprs.isEmpty then null
      else exprs.foldLeft[Any](null)((_, e) => eval(e, ctx))

    // ── Control flow ──────────────────────────────────────────────────────────

    case IfExpr(cond, thenB, elseIfs, elseBranch, _) =>
      if Operators.toBoolean(eval(cond, ctx)) then eval(thenB, ctx)
      else
        elseIfs.find(ei => Operators.toBoolean(eval(ei._1, ctx))) match
          case Some((_, body)) => eval(body, ctx)
          case None => elseBranch.map(eval(_, ctx)).getOrElse(null)

    case WhileExpr(cond, body, _) =>
      var result: Any = null
      while Operators.toBoolean(eval(cond, ctx)) do
        try result = eval(body, ctx)
        catch
          case _: ContinueException.type => ()
          case e: BreakException => return e.value
      result

    case DoWhileExpr(body, cond, _) =>
      var result: Any = null
      var running = true
      while running do
        try result = eval(body, ctx)
        catch
          case _: ContinueException.type => ()
          case e: BreakException => return e.value
        running = Operators.toBoolean(eval(cond, ctx))
      result

    case ForExpr(init, cond, update, body, _) =>
      ctx.openScope()
      try
        init.foreach(eval(_, ctx))
        var result: Any = null
        var running = cond.map(c => Operators.toBoolean(eval(c, ctx))).getOrElse(true)
        while running do
          try result = eval(body, ctx)
          catch
            case _: ContinueException.type => ()
            case e: BreakException => return e.value
          update.foreach(eval(_, ctx))
          running = cond.map(c => Operators.toBoolean(eval(c, ctx))).getOrElse(true)
        result
      finally ctx.closeScope()

    case ForEachExpr(vars, iterable, body, pos) =>
      val col = eval(iterable, ctx)
      var result: Any = null
      ctx.openScope()
      try
        forEachOn(vars, col, body, ctx, pos, (v) => result = v)
      finally ctx.closeScope()
      result

    case ForeachExpr(varName, iterable, body, pos) =>
      val col = eval(iterable, ctx)
      var result: Any = null
      ctx.openScope()
      try
        forEachOn(List(varName), col, body, ctx, pos, (v) => result = v)
      finally ctx.closeScope()
      result

    case SwitchExpr(target, cases, _) =>
      val v = eval(target, ctx)
      var result: Any = null
      try
        var matched = false
        for c <- cases do
          if !matched then
            matched = c.labels.exists {
              case None    => true  // default
              case Some(e) => Operators.eq(v, eval(e, ctx))
            }
          if matched then
            result = eval(c.body, ctx)
      catch
        case e: BreakException => return e.value
      result

    // ── Functions ─────────────────────────────────────────────────────────────

    case FuncDef(name, params, varargs, body, _) =>
      val f = PnutsFunc(
        name, params.toArray, varargs, body,
        ctx.currentPackage,
        ctx.stackFrame.map(_.makeLexicalScope()).getOrElse(Map.empty),
      )
      val group = name match
        case Some(n) =>
          // Look for existing group in current scope, or create new one
          val existing = ctx.stackFrame.flatMap(_.lookup(n)).map(_.value) match
            case Some(g: PnutsGroup) => g
            case _ =>
              ctx.currentPackage.lookup(n).map(_.value) match
                case Some(g: PnutsGroup) => g
                case _ => PnutsGroup(Some(n))
          existing.register(f)
          if ctx.stackFrame.isDefined then ctx.setValue(n, existing)
          else ctx.currentPackage.set(n, existing)
          existing
        case None =>
          val g = PnutsGroup(None)
          g.register(f)
          g
      group

    case ReturnExpr(value, _) =>
      throw ReturnException(value.map(eval(_, ctx)).getOrElse(null))

    case BreakExpr(value, _) =>
      throw BreakException(value.map(eval(_, ctx)).getOrElse(null))

    case ContinueExpr(_) =>
      throw ContinueException

    case YieldExpr(value, _) =>
      val v = value.map(eval(_, ctx)).getOrElse(null)
      if ctx.yieldBuf == null then ctx.yieldBuf = new java.util.ArrayList[Any]()
      ctx.yieldBuf.add(v)
      null

    case RangeExpr(from, to, _) =>
      val f = Operators.toLong(eval(from, ctx))
      val t = Operators.toLong(eval(to, ctx))
      val n = ((t - f) + 1).toInt max 0
      val arr = new Array[Any](n)
      for i <- 0 until n do arr(i) = f + i
      arr

    // ── Function call ─────────────────────────────────────────────────────────

    case FuncCall(func, args, pos) =>
      val f = eval(func, ctx)
      val argVals = args.map(eval(_, ctx)).toArray
      callValue(f, argVals, ctx, pos)

    case MethodCall(obj, method, args, pos) =>
      val target  = eval(obj, ctx)
      val argVals = args.map(eval(_, ctx)).toArray
      callMethod(target, method, argVals, ctx, pos)

    case StaticMethodCall(obj, method, args, pos) =>
      val target  = eval(obj, ctx)
      val argVals = args.map(eval(_, ctx)).toArray
      callMethod(target, method, argVals, ctx, pos)

    case MemberAccess(obj, member, pos) =>
      val target = eval(obj, ctx)
      getField(target, member, ctx, pos)

    case StaticMemberAccess(obj, member, pos) =>
      val target = eval(obj, ctx)
      getField(target, member, ctx, pos)

    case IndexAccess(obj, index, pos) =>
      val target = eval(obj, ctx)
      val idx    = eval(index, ctx)
      getElement(target, idx, pos)

    case RangeAccess(obj, from, to, pos) =>
      val target = eval(obj, ctx)
      val f = eval(from, ctx)
      val t = to.map(eval(_, ctx))
      getRange(target, f, t, pos)

    // ── Exception handling ─────────────────────────────────────────────────────

    case TryExpr(body, catches, fin, _) =>
      var result: Any = null
      try
        result = eval(body, ctx)
      catch
        case e: Throwable if !e.isInstanceOf[ReturnException] &&
                             !e.isInstanceOf[BreakException]  &&
                             !e.isInstanceOf[ContinueException.type] =>
          val matched = catches.find { c =>
            val cls = resolveClass(c.typeName, ctx, c.pos)
            cls.isInstance(e)
          }
          matched match
            case Some(c) =>
              ctx.openScope()
              try
                ctx.setValue(c.varName, e)
                result = eval(c.body, ctx)
              finally ctx.closeScope()
            case None => throw e
      finally
        fin.foreach(eval(_, ctx))
      result

    case ThrowExpr(expr, pos) =>
      val v = expr.map(eval(_, ctx)).getOrElse(null)
      v match
        case t: Throwable => throw t
        case _            => throw new RuntimeException(Operators.toStr(v))

    // ── Package / Import ───────────────────────────────────────────────────────

    case PackageExpr(parts, dynamic, _) =>
      val name = dynamic.map(e => Operators.toStr(eval(e, ctx)))
                        .getOrElse(parts.mkString("."))
      // navigate to or create the package
      val pkg = name.split('.').foldLeft(PnutsPackage.global)(_.child(_))
      ctx.currentPackage = pkg
      pkg

    case ImportExpr(parts, wildcard, isStatic, dynamic, _) =>
      val importStr = dynamic.map(e => Operators.toStr(eval(e, ctx)))
                             .getOrElse(parts.mkString(".") + (if wildcard then ".*" else ""))
      ctx.imports += importStr
      importStr

    // ── Class / New (JVM-specific stubs for Phase 1) ──────────────────────────

    case ClassRef(name, pos) =>
      resolveClass(name, ctx, pos)

    case ClassExpr(expr, pos) =>
      val v = eval(expr, ctx)
      v match
        case s: String => resolveClass(List(s), ctx, pos)
        case c: Class[?] => c
        case _ => throw RuntimeError(s"class() requires a class name, got $v", pos)

    case NewExpr(className, dims, args, body, pos) =>
      val cls = resolveClass(className, ctx, pos)
      if dims.nonEmpty then
        // Array creation: new Type[n]
        val size = Operators.toLong(eval(dims.head, ctx)).toInt
        java.lang.reflect.Array.newInstance(cls, size)
      else
        val argVals = args.map(eval(_, ctx)).toArray
        callConstructor(cls, argVals, pos)

    case CastExpr(typeName, _, expr, pos) =>
      val v   = eval(expr, ctx)
      val cls = resolveClass(typeName, ctx, pos)
      castValue(v, cls, pos)

    // ── Bean / class def (stubs) ───────────────────────────────────────────────

    case BeanDef(typeName, props, pos) =>
      val cls = resolveClass(typeName, ctx, pos)
      val obj = cls.getDeclaredConstructor().newInstance()
      for p <- props do
        setProperty(obj, p.name, eval(p.value, ctx), pos)
      obj

    case ClassDef(name, _, _, _, _) =>
      // Phase 3: delegate to bytecode compiler
      throw UnsupportedOperationError("class definition requires Phase 3 compiler")

    // CatchExpr / FinallyExpr: functional exception-handling forms
    case CatchExpr(cls, handler, pos) =>
      val clsVal    = eval(cls, ctx)
      val handlerFn = eval(handler, ctx)
      // Returns a function that wraps its argument body in try/catch
      NativeFunc.vararg("$catch") { (args, c) =>
        try
          if args.length > 0 then c.callFn(args(0), Array.empty, c, pos)
          else null
        catch
          case e: Throwable
              if !e.isInstanceOf[ReturnException] &&
                 !e.isInstanceOf[BreakException]  &&
                 !e.isInstanceOf[ContinueException.type] &&
                 clsVal.asInstanceOf[Class[?]].isInstance(e) =>
            c.callFn(handlerFn, Array(e), c, pos)
      }

    case FinallyExpr(body, finalizer, pos) =>
      try eval(body, ctx)
      finally finalizer.foreach(eval(_, ctx))

    case e =>
      throw UnsupportedOperationError(s"Unimplemented AST node: ${e.getClass.getSimpleName}")

  // ── private helpers ────────────────────────────────────────────────────────

  private def callValue(f: Any, args: Array[Any], ctx: Context, pos: spnuts.ast.SourcePos): Any =
    f match
      case group: PnutsGroup =>
        group.resolve(args.length) match
          case Some(func) => callAny(func, args, ctx, pos)
          case None => throw RuntimeError(
            s"No overload of '${group.name.getOrElse("?")}' for ${args.length} arguments", pos)
      case n: NativeFunc =>
        n.impl(args, ctx)
      case cls: Class[?] =>
        callConstructor(cls, args, pos)
      case null =>
        throw RuntimeError("Cannot call null as a function", pos)
      case _ =>
        throw RuntimeError(s"Not callable: $f (${f.getClass.getSimpleName})", pos)

  private def callAny(f: AnyFunc, args: Array[Any], ctx: Context, pos: spnuts.ast.SourcePos): Any =
    f match
      case func: PnutsFunc    => callFunction(func, args, ctx, pos)
      case native: NativeFunc => native.impl(args, ctx)

  private def callFunction(func: PnutsFunc, args: Array[Any], ctx: Context, pos: spnuts.ast.SourcePos): Any =
    val savedFrame    = ctx.stackFrame
    val savedPackage  = ctx.currentPackage
    val savedYieldBuf = ctx.yieldBuf
    ctx.yieldBuf = null  // fresh yield buffer for this call
    ctx.openFrame(func, args)
    ctx.currentPackage = func.pkg
    try
      val result = try eval(func.body, ctx) catch { case e: ReturnException => e.value }
      // If any yield was called, return accumulated values as array
      val buf = ctx.yieldBuf
      if buf != null && !buf.isEmpty then buf.toArray()
      else result
    finally
      ctx.closeFrame(savedFrame)
      ctx.currentPackage = savedPackage
      ctx.yieldBuf = savedYieldBuf

  private def callMethod(target: Any, method: String, args: Array[Any], ctx: Context, pos: spnuts.ast.SourcePos): Any =
    // First try calling as a Pnuts function in the package
    val pkg = target match
      case p: PnutsPackage => p
      case _ => null
    if pkg != null then
      pkg.lookup(method) match
        case Some(b) => return callValue(b.value, args, ctx, pos)
        case None => ()

    // Java reflection
    if target == null then throw RuntimeError("Cannot call method on null", pos)
    val cls = target.getClass
    JavaInteropShim.callMethod(cls, target, method, args, pos)

  private def getField(target: Any, member: String, ctx: Context, pos: spnuts.ast.SourcePos): Any =
    if target == null then throw RuntimeError("Cannot access field on null", pos)
    target match
      case p: PnutsPackage =>
        p.lookup(member).map(_.value).getOrElse(null)
      case arr: Array[?] if member == "length" => arr.length
      case _ =>
        JavaInteropShim.getField(target, member, pos)

  private def getElement(target: Any, idx: Any, pos: spnuts.ast.SourcePos): Any =
    target match
      case arr: Array[?] =>
        arr(Operators.toLong(idx).toInt)
      case list: java.util.List[?] =>
        list.get(Operators.toLong(idx).toInt)
      case map: java.util.Map[?, ?] =>
        map.get(idx)
      case s: String =>
        s.charAt(Operators.toLong(idx).toInt)
      case _ =>
        throw RuntimeError(s"Cannot index into ${target.getClass.getSimpleName}", pos)

  private def getRange(target: Any, from: Any, to: Option[Any], pos: spnuts.ast.SourcePos): Any =
    val start = Operators.toLong(from).toInt
    target match
      case arr: Array[?] =>
        // arr[a..b] is inclusive on both ends (like Ruby)
        val end = to.map(v => Operators.toLong(v).toInt + 1).getOrElse(arr.length)
        arr.slice(start, end)
      case s: String =>
        val end = to.map(v => Operators.toLong(v).toInt + 1).getOrElse(s.length)
        s.substring(start, end min s.length)
      case _ =>
        throw RuntimeError(s"Range access not supported for ${target.getClass.getSimpleName}", pos)

  private def assignTo(lhs: Expr, value: Any, ctx: Context, pos: spnuts.ast.SourcePos): Unit =
    lhs match
      case Ident(name, _)        => ctx.setValue(name, value)
      case GlobalRef(name, _)    => PnutsPackage.global.set(name, value)
      case IndexAccess(obj, idx, p) =>
        val target = eval(obj, ctx)
        val i = eval(idx, ctx)
        setElement(target, i, value, p)
      case MemberAccess(obj, member, p) =>
        val target = eval(obj, ctx)
        JavaInteropShim.setField(target, member, value, p)
      case _ =>
        throw RuntimeError(s"Invalid assignment target: ${lhs.getClass.getSimpleName}", pos)

  private def computeAssign(op: AssignOp, lhs: Expr, rhs: Expr, ctx: Context, pos: spnuts.ast.SourcePos): Any =
    import AssignOp.*
    op match
      case Assign => eval(rhs, ctx)
      case _ =>
        val current = eval(lhs, ctx)
        val rhsVal  = eval(rhs, ctx)
        op match
          case AddAssign => Operators.add(current, rhsVal)
          case SubAssign => Operators.sub(current, rhsVal)
          case MulAssign => Operators.mul(current, rhsVal)
          case DivAssign => Operators.div(current, rhsVal)
          case ModAssign => Operators.mod(current, rhsVal)
          case AndAssign => Operators.bitAnd(current, rhsVal)
          case OrAssign  => Operators.bitOr(current, rhsVal)
          case XorAssign => Operators.bitXor(current, rhsVal)
          case ShiftLeftAssign => Operators.shl(current, rhsVal)
          case ShiftRightAssign => Operators.shr(current, rhsVal)
          case UnsignedShiftRightAssign => Operators.ushr(current, rhsVal)
          case Assign => eval(rhs, ctx)

  private def doIncr(target: Expr, ctx: Context, delta: Long, returnOld: Boolean): Any =
    val old = eval(target, ctx)
    val newVal = Operators.add(old, delta)
    assignTo(target, newVal, ctx, target.pos)
    if returnOld then old else newVal

  private def setElement(target: Any, idx: Any, value: Any, pos: spnuts.ast.SourcePos): Unit =
    target match
      case arr: Array[Any] => arr(Operators.toLong(idx).toInt) = value
      case list: java.util.List[Any] @unchecked => list.set(Operators.toLong(idx).toInt, value)
      case map: java.util.Map[Any, Any] @unchecked => map.put(idx, value)
      case _ => throw RuntimeError(s"Cannot set element on ${target.getClass.getSimpleName}", pos)

  private def forEachOn(
    vars: List[String], col: Any, body: Expr, ctx: Context,
    pos: spnuts.ast.SourcePos, onResult: Any => Unit
  ): Unit =
    col match
      case arr: Array[?] =>
        for item <- arr do
          vars.head match { case v => ctx.setValue(v, item) }
          try onResult(eval(body, ctx))
          catch
            case _: ContinueException.type => ()
            case e: BreakException => return
      case iter: java.lang.Iterable[?] =>
        val it = iter.iterator()
        while it.hasNext do
          val item = it.next()
          ctx.setValue(vars.head, item)
          try onResult(eval(body, ctx))
          catch
            case _: ContinueException.type => ()
            case e: BreakException => return
      case _ =>
        throw RuntimeError(s"Cannot iterate over ${col.getClass.getSimpleName}", pos)

  private def resolveClass(parts: List[String], ctx: Context, pos: spnuts.ast.SourcePos): Class[?] =
    val name = parts.mkString(".")
    JavaInteropShim.resolveClass(name, ctx.imports.toList)
      .getOrElse(throw RuntimeError(s"Cannot resolve class: $name", pos))

  private def callConstructor(cls: Class[?], args: Array[Any], pos: spnuts.ast.SourcePos): Any =
    JavaInteropShim.callConstructor(cls, args, pos)

  private def castValue(v: Any, cls: Class[?], pos: spnuts.ast.SourcePos): Any =
    if cls.isInstance(v) then v
    else if cls == classOf[Long] || cls == classOf[java.lang.Long] then Operators.toLong(v)
    else if cls == classOf[Double] || cls == classOf[java.lang.Double] then Operators.toDouble(v)
    else if cls == classOf[Int] || cls == classOf[java.lang.Integer] then Operators.toLong(v).toInt
    else if cls == classOf[String] then Operators.toStr(v)
    else cls.cast(v)

  private def setProperty(obj: Any, name: String, value: Any, pos: spnuts.ast.SourcePos): Unit =
    JavaInteropShim.setField(obj, name, value, pos)

  private def toDouble(v: Any): Double = Operators.toDouble(v)

// ── Errors ─────────────────────────────────────────────────────────────────────

case class RuntimeError(
  msg: String,
  pos: spnuts.ast.SourcePos,
  cause: Throwable = null
) extends Exception(s"$pos: $msg", cause)

case class UnsupportedOperationError(msg: String) extends Exception(msg)
