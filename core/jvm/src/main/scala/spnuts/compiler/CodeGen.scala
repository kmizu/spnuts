package spnuts.compiler

import org.objectweb.asm.{ClassWriter, MethodVisitor, Label, Opcodes, Type}
import org.objectweb.asm.Opcodes.*
import spnuts.ast.*
import spnuts.runtime.Operators

/**
 * ASM-based code generator for SPnuts expressions.
 *
 * Compiles an AST subtree into JVM bytecode via a MethodVisitor.
 * All values on the JVM operand stack are typed as `Object` (boxed).
 *
 * Variable layout in JVM locals:
 *   slot 0     = Context parameter ("ctx")
 *   slot 1..n  = function parameters (if compiling a function body)
 *   slot n+1.. = other locals allocated on demand
 *
 * Variables not in `scope.locals` are accessed via Context.getValue / setValue.
 */
/**
 * @param slotBase extra offset added to all param/local slot numbers.
 *   0 for scripts (ctx=0, params would start at 1, extras from 1+params.length).
 *   1 for functions (ctx=0, args[]=1, params start at 2, extras from 2+params.length).
 */
class CodeGen(mv: MethodVisitor, scope: ScopeAnalyzer.ScopeInfo, slotBase: Int = 0):

  import CodeGen.*

  // next free JVM local slot (after ctx + optional args[] + params)
  private val nextSlot = new java.util.concurrent.atomic.AtomicInteger(
    scope.params.length + 1 + slotBase)
  // map extra local vars to their JVM slot
  protected val extraLocals = collection.mutable.HashMap.empty[String, Int]

  // exception types used for Pnuts control flow
  private val RETURN_EX  = "spnuts/runtime/ReturnException"
  private val BREAK_EX   = "spnuts/runtime/BreakException"
  private val CONT_EX    = "spnuts/runtime/ContinueException$"

  // ── Public API ─────────────────────────────────────────────────────────────

  /** Compile `expr`, leaving its value on the operand stack (Object). */
  def compileExpr(expr: Expr): Unit = expr match

    // ── Literals ─────────────────────────────────────────────────────────────

    case IntLit(n, _, _) =>
      val v = n match
        case l: Long => l
        case i: Int  => i.toLong
        case _       => 0L
      mv.visitLdcInsn(v)
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)

    case FloatLit(d, _, _) =>
      val v = d match
        case dbl: Double => dbl
        case flt: Float  => flt.toDouble
        case _           => 0.0
      mv.visitLdcInsn(v)
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)

    case StringLit(s, _) =>
      mv.visitLdcInsn(s)

    case BoolLit(b, _) =>
      val field = if b then "TRUE" else "FALSE"
      mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", field, "Ljava/lang/Boolean;")

    case NullLit(_) =>
      mv.visitInsn(ACONST_NULL)

    case CharLit(c, _) =>
      mv.visitLdcInsn(c.toInt)
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false)

    // ── Identifiers ───────────────────────────────────────────────────────────

    case Ident(name, _) =>
      scope.slotOf(name) match
        case Some(slot) =>
          mv.visitVarInsn(ALOAD, slot + slotBase)
        case None =>
          extraLocals.get(name) match
            case Some(slot) =>
              mv.visitVarInsn(ALOAD, slot)
            case None =>
              // From Context
              mv.visitVarInsn(ALOAD, 0)  // ctx
              mv.visitLdcInsn(name)
              mv.visitMethodInsn(INVOKEVIRTUAL, CTX_CLS, "getValue", "(Ljava/lang/String;)Ljava/lang/Object;", false)

    case GlobalRef(name, _) =>
      // PnutsPackage.global.lookup(name).map(_.value).getOrElse(null)
      mv.visitFieldInsn(GETSTATIC, PKG_CLS + "$", "MODULE$", "L" + PKG_CLS + "$/;")
      mv.visitMethodInsn(INVOKEVIRTUAL, PKG_CLS + "$", "global", "()L" + PKG_CLS + ";", false)
      mv.visitLdcInsn(name)
      mv.visitMethodInsn(INVOKEVIRTUAL, PKG_CLS, "lookup",
        "(Ljava/lang/String;)Lscala/Option;", false)
      mv.visitMethodInsn(INVOKEVIRTUAL, "scala/Option", "getOrElse",
        "(Lscala/Function0;)Ljava/lang/Object;", false)

    // ── Operators ─────────────────────────────────────────────────────────────

    case BinaryExpr(op, lhs, rhs, _) =>
      op match
        case BinOp.LogAnd => compileShortCircuit(lhs, rhs, isAnd = true)
        case BinOp.LogOr  => compileShortCircuit(lhs, rhs, isAnd = false)
        case BinOp.NotEq =>
          compileExpr(lhs); compileExpr(rhs)
          callOperatorsBool("eq", 2)
          // flip: Z on stack, convert to !result
          val trueLabel = new Label; val endLabel = new Label
          mv.visitJumpInsn(IFNE, trueLabel)
          mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;")
          mv.visitJumpInsn(GOTO, endLabel)
          mv.visitLabel(trueLabel)
          mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;")
          mv.visitLabel(endLabel)
        case BinOp.Eq => compileExpr(lhs); compileExpr(rhs); callOperatorsBoolBoxed("eq", 2)
        case BinOp.Lt => compileExpr(lhs); compileExpr(rhs); callOperatorsBoolBoxed("lt", 2)
        case BinOp.Gt => compileExpr(lhs); compileExpr(rhs); callOperatorsBoolBoxed("gt", 2)
        case BinOp.Le => compileExpr(lhs); compileExpr(rhs); callOperatorsBoolBoxed("le", 2)
        case BinOp.Ge => compileExpr(lhs); compileExpr(rhs); callOperatorsBoolBoxed("ge", 2)
        case _ =>
          compileExpr(lhs); compileExpr(rhs)
          val methodName = op match
            case BinOp.Add => "add"; case BinOp.Sub => "sub"; case BinOp.Mul => "mul"
            case BinOp.Div => "div"; case BinOp.Mod => "mod"
            case BinOp.BitAnd => "bitAnd"; case BinOp.BitOr => "bitOr"; case BinOp.BitXor => "bitXor"
            case BinOp.ShiftLeft => "shl"; case BinOp.ShiftRight => "shr"
            case BinOp.UnsignedShiftRight => "ushr"
            case _ => throw UnsupportedOperationException(s"BinOp $op")
          callOperators(methodName, 2)

    case UnaryExpr(op, operand, _) =>
      op match
        case UnaryOp.Neg    => compileExpr(operand); callOperators("neg", 1)
        case UnaryOp.BitNot => compileExpr(operand); callOperators("bitNot", 1)
        case UnaryOp.LogNot =>
          compileExpr(operand)
          callOperatorsToBoolean()
          val t = new Label; val e = new Label
          mv.visitJumpInsn(IFEQ, t)
          mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;")
          mv.visitJumpInsn(GOTO, e)
          mv.visitLabel(t)
          mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;")
          mv.visitLabel(e)
        case UnaryOp.PreIncr  => compileIncr(operand, delta = 1L, returnOld = false)
        case UnaryOp.PreDecr  => compileIncr(operand, delta = -1L, returnOld = false)
        case UnaryOp.PostIncr => compileIncr(operand, delta = 1L, returnOld = true)
        case UnaryOp.PostDecr => compileIncr(operand, delta = -1L, returnOld = true)

    case TernaryExpr(cond, thenE, elseE, _) =>
      compileExpr(cond)
      callOperatorsToBoolean()
      val elseLabel = new Label; val endLabel = new Label
      mv.visitJumpInsn(IFEQ, elseLabel)
      compileExpr(thenE)
      mv.visitJumpInsn(GOTO, endLabel)
      mv.visitLabel(elseLabel)
      compileExpr(elseE)
      mv.visitLabel(endLabel)

    // ── Assignment ────────────────────────────────────────────────────────────

    case Assignment(op, lhs, rhs, _) =>
      val value: Any = op match
        case AssignOp.Assign => compileExpr(rhs); null
        case _ =>
          // compound: load current + compute new value
          compileExpr(lhs)
          compileExpr(rhs)
          val methodName = op match
            case AssignOp.AddAssign => "add"; case AssignOp.SubAssign => "sub"
            case AssignOp.MulAssign => "mul"; case AssignOp.DivAssign => "div"
            case AssignOp.ModAssign => "mod"
            case AssignOp.AndAssign => "bitAnd"; case AssignOp.OrAssign => "bitOr"
            case AssignOp.XorAssign => "bitXor"
            case AssignOp.ShiftLeftAssign => "shl"; case AssignOp.ShiftRightAssign => "shr"
            case AssignOp.UnsignedShiftRightAssign => "ushr"
            case _ => "add"
          callOperators(methodName, 2)
          null
      // now value is on stack; store it
      storeToLhs(lhs)

    case MultiAssign(targets, rhs, _) =>
      compileExpr(rhs)
      // result might be array; for each target, getElement and store
      val arrSlot = nextSlot.getAndIncrement()
      mv.visitVarInsn(ASTORE, arrSlot)
      for (t, i) <- targets.zipWithIndex do
        mv.visitVarInsn(ALOAD, arrSlot)
        mv.visitLdcInsn(i.toLong)
        mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "getElement",
          "(Ljava/lang/Object;J)Ljava/lang/Object;", false)
        storeVar(t.name, isVoid = true)
      mv.visitInsn(ACONST_NULL)

    // ── Control flow ──────────────────────────────────────────────────────────

    case IfExpr(cond, thenE, elseIfs, elseE, _) =>
      val endLabel = new Label
      // main if
      compileExpr(cond)
      callOperatorsToBoolean()
      val nextLabel = new Label
      mv.visitJumpInsn(IFEQ, nextLabel)
      compileExpr(thenE)
      mv.visitJumpInsn(GOTO, endLabel)
      mv.visitLabel(nextLabel)
      // else-if chain
      for (ec, et) <- elseIfs do
        val skip = new Label
        compileExpr(ec)
        callOperatorsToBoolean()
        mv.visitJumpInsn(IFEQ, skip)
        compileExpr(et)
        mv.visitJumpInsn(GOTO, endLabel)
        mv.visitLabel(skip)
      // else
      elseE match
        case Some(e) => compileExpr(e)
        case None    => mv.visitInsn(ACONST_NULL)
      mv.visitLabel(endLabel)

    case WhileExpr(cond, body, _) =>
      val startLabel = new Label; val endLabel = new Label
      mv.visitLabel(startLabel)
      compileExpr(cond)
      callOperatorsToBoolean()
      mv.visitJumpInsn(IFEQ, endLabel)
      compileStmt(body)
      mv.visitJumpInsn(GOTO, startLabel)
      mv.visitLabel(endLabel)
      mv.visitInsn(ACONST_NULL)

    case DoWhileExpr(body, cond, _) =>
      val startLabel = new Label
      mv.visitLabel(startLabel)
      compileStmt(body)
      compileExpr(cond)
      callOperatorsToBoolean()
      mv.visitJumpInsn(IFNE, startLabel)
      mv.visitInsn(ACONST_NULL)

    case ForEachExpr(vars, iterable, body, _) =>
      compileExpr(iterable)
      val iterSlot = nextSlot.getAndIncrement()
      mv.visitVarInsn(ASTORE, iterSlot)
      val iterableSlot = iterSlot
      // delegate to runtime helper for iteration
      val varName = vars.head
      val startLabel = new Label; val endLabel = new Label
      // Get iterator from iterable (helper returns Iterator or null when done)
      mv.visitVarInsn(ALOAD, iterableSlot)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "makeIterator",
        "(Ljava/lang/Object;)Ljava/util/Iterator;", false)
      val itSlot = nextSlot.getAndIncrement()
      mv.visitVarInsn(ASTORE, itSlot)
      mv.visitLabel(startLabel)
      mv.visitVarInsn(ALOAD, itSlot)
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true)
      mv.visitJumpInsn(IFEQ, endLabel)
      mv.visitVarInsn(ALOAD, itSlot)
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true)
      // store to loop var
      storeVar(varName, isVoid = true)
      compileStmt(body)
      mv.visitJumpInsn(GOTO, startLabel)
      mv.visitLabel(endLabel)
      mv.visitInsn(ACONST_NULL)

    case ForExpr(init, cond, update, body, _) =>
      init.foreach(compileStmt)
      val startLabel = new Label; val endLabel = new Label
      mv.visitLabel(startLabel)
      cond match
        case Some(c) =>
          compileExpr(c)
          callOperatorsToBoolean()
          mv.visitJumpInsn(IFEQ, endLabel)
        case None => ()
      compileStmt(body)
      update.foreach(compileStmt)
      mv.visitJumpInsn(GOTO, startLabel)
      mv.visitLabel(endLabel)
      mv.visitInsn(ACONST_NULL)

    // ── Control-flow exceptions ────────────────────────────────────────────────

    case ReturnExpr(value, _) =>
      val v = value.getOrElse(NullLit(expr.pos))
      compileExpr(v)
      // throw new ReturnException(value)
      mv.visitInsn(DUP)
      mv.visitTypeInsn(NEW, RETURN_EX)
      mv.visitInsn(DUP)
      mv.visitInsn(DUP2_X1)  // rearrange: ReturnException, ReturnException, value
      mv.visitInsn(POP2)
      mv.visitMethodInsn(INVOKESPECIAL, RETURN_EX, "<init>", "(Ljava/lang/Object;)V", false)
      mv.visitInsn(ATHROW)

    case BreakExpr(value, _) =>
      val v = value.getOrElse(NullLit(expr.pos))
      compileExpr(v)
      mv.visitTypeInsn(NEW, BREAK_EX)
      mv.visitInsn(DUP)
      mv.visitInsn(DUP2_X1)
      mv.visitInsn(POP2)
      mv.visitMethodInsn(INVOKESPECIAL, BREAK_EX, "<init>", "(Ljava/lang/Object;)V", false)
      mv.visitInsn(ATHROW)

    case ContinueExpr(_) =>
      mv.visitFieldInsn(GETSTATIC, CONT_EX, "MODULE$", "L" + CONT_EX + ";")
      mv.visitInsn(ATHROW)

    case YieldExpr(value, _) =>
      // ctx.yieldBuf check + add — delegate via helper
      compileExpr(value.getOrElse(NullLit(expr.pos)))
      mv.visitVarInsn(ALOAD, 0)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "yield_",
        "(Ljava/lang/Object;L" + CTX_CLS + ";)V", false)
      mv.visitInsn(ACONST_NULL)

    case ThrowExpr(value, _) =>
      compileExpr(value.getOrElse(NullLit(expr.pos)))
      // wrap non-Throwable in RuntimeException
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "throwValue",
        "(Ljava/lang/Object;)Ljava/lang/Throwable;", false)
      mv.visitInsn(ATHROW)

    // ── Blocks ────────────────────────────────────────────────────────────────

    case Block(Nil, _) | ExprList(Nil, _) =>
      mv.visitInsn(ACONST_NULL)

    case Block(exprs, _) => compileExprList(exprs)

    case ExprList(exprs, _) => compileExprList(exprs)

    // ── Function call ─────────────────────────────────────────────────────────

    case FuncCall(func, args, pos) =>
      compileExpr(func)
      compileArgArray(args)
      mv.visitVarInsn(ALOAD, 0)  // ctx
      mv.visitLdcInsn(pos.file)
      mv.visitLdcInsn(pos.line)
      mv.visitLdcInsn(pos.column)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "callFunc",
        "(Ljava/lang/Object;[Ljava/lang/Object;L" + CTX_CLS + ";Ljava/lang/String;II)Ljava/lang/Object;", false)

    case MethodCall(obj, method, args, pos) =>
      compileExpr(obj)
      mv.visitLdcInsn(method)
      compileArgArray(args)
      mv.visitVarInsn(ALOAD, 0)  // ctx
      mv.visitLdcInsn(pos.file)
      mv.visitLdcInsn(pos.line)
      mv.visitLdcInsn(pos.column)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "callMethod",
        "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;L" + CTX_CLS + ";Ljava/lang/String;II)Ljava/lang/Object;", false)

    case StaticMethodCall(obj, method, args, pos) =>
      compileExpr(obj)
      mv.visitLdcInsn(method)
      compileArgArray(args)
      mv.visitVarInsn(ALOAD, 0)
      mv.visitLdcInsn(pos.file)
      mv.visitLdcInsn(pos.line)
      mv.visitLdcInsn(pos.column)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "callMethod",
        "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;L" + CTX_CLS + ";Ljava/lang/String;II)Ljava/lang/Object;", false)

    // ── Field access ──────────────────────────────────────────────────────────

    case MemberAccess(obj, member, pos) =>
      compileExpr(obj)
      mv.visitLdcInsn(member)
      mv.visitVarInsn(ALOAD, 0)
      mv.visitLdcInsn(pos.file); mv.visitLdcInsn(pos.line); mv.visitLdcInsn(pos.column)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "getField",
        "(Ljava/lang/Object;Ljava/lang/String;L" + CTX_CLS + ";Ljava/lang/String;II)Ljava/lang/Object;", false)

    case StaticMemberAccess(obj, member, pos) =>
      compileExpr(obj)
      mv.visitLdcInsn(member)
      mv.visitVarInsn(ALOAD, 0)
      mv.visitLdcInsn(pos.file); mv.visitLdcInsn(pos.line); mv.visitLdcInsn(pos.column)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "getField",
        "(Ljava/lang/Object;Ljava/lang/String;L" + CTX_CLS + ";Ljava/lang/String;II)Ljava/lang/Object;", false)

    case IndexAccess(obj, idx, pos) =>
      compileExpr(obj)
      compileExpr(idx)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "getElement",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false)

    case RangeAccess(obj, from, to, _) =>
      compileExpr(obj)
      compileExpr(from)
      to match
        case Some(t) => compileExpr(t)
        case None    => mv.visitInsn(ACONST_NULL)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "getRange",
        "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false)

    // ── Collections ───────────────────────────────────────────────────────────

    case ListExpr(elems, _, _) =>
      compileArgArray(elems)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "makeList",
        "([Ljava/lang/Object;)Ljava/lang/Object;", false)

    case MapExpr(pairs, _) =>
      mv.visitLdcInsn(pairs.length * 2)
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object")
      for ((k, v), i) <- pairs.zipWithIndex do
        mv.visitInsn(DUP)
        mv.visitLdcInsn(i * 2)
        compileExpr(k)
        mv.visitInsn(AASTORE)
        mv.visitInsn(DUP)
        mv.visitLdcInsn(i * 2 + 1)
        compileExpr(v)
        mv.visitInsn(AASTORE)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "makeMap",
        "([Ljava/lang/Object;)Ljava/lang/Object;", false)

    case RangeExpr(from, to, _) =>
      compileExpr(from)
      compileExpr(to)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "makeRange",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false)

    // ── Function definition ───────────────────────────────────────────────────

    case fd @ FuncDef(name, params, varargs, body, pos) =>
      // For closures that capture from outer scope or complex bodies,
      // delegate to the Interpreter at runtime. (Simpler than compiling nested classes.)
      // We generate code that calls a runtime helper to create a PnutsFunc.
      // The body AST is stored in a static field of the generated class.
      // For now: serialize name, params, varargs; defer body to interpreter path.
      mv.visitVarInsn(ALOAD, 0)  // ctx
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "defineFuncStub",
        "(L" + CTX_CLS + ";)Ljava/lang/Object;", false)
      // This is a stub — the real compilation of FuncDef is done in Compiler.compileFuncDef

    // ── Java interop ─────────────────────────────────────────────────────────

    case NewExpr(className, dims, args, _, pos) =>
      mv.visitVarInsn(ALOAD, 0)
      mv.visitLdcInsn(className.mkString("."))
      compileArgArray(args)
      mv.visitLdcInsn(pos.file); mv.visitLdcInsn(pos.line); mv.visitLdcInsn(pos.column)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "newObject",
        "(L" + CTX_CLS + ";Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/String;II)Ljava/lang/Object;", false)

    case CastExpr(typeName, _, e, pos) =>
      compileExpr(e)
      mv.visitVarInsn(ALOAD, 0)
      mv.visitLdcInsn(typeName.mkString("."))
      mv.visitLdcInsn(pos.file); mv.visitLdcInsn(pos.line); mv.visitLdcInsn(pos.column)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "castValue",
        "(Ljava/lang/Object;L" + CTX_CLS + ";Ljava/lang/String;Ljava/lang/String;II)Ljava/lang/Object;", false)

    case InstanceofExpr(e, typeName, pos) =>
      compileExpr(e)
      mv.visitVarInsn(ALOAD, 0)
      mv.visitLdcInsn(typeName.mkString("."))
      mv.visitLdcInsn(pos.file); mv.visitLdcInsn(pos.line); mv.visitLdcInsn(pos.column)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "instanceofCheck",
        "(Ljava/lang/Object;L" + CTX_CLS + ";Ljava/lang/String;Ljava/lang/String;II)Ljava/lang/Object;", false)

    // ── String interpolation ──────────────────────────────────────────────────

    case InterpolatedString(parts, _) =>
      mv.visitLdcInsn(parts.length)
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object")
      for (p, i) <- parts.zipWithIndex do
        mv.visitInsn(DUP)
        mv.visitLdcInsn(i)
        p match
          case Left(s)  => mv.visitLdcInsn(s)
          case Right(e) => compileExpr(e)
        mv.visitInsn(AASTORE)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "interpolate",
        "([Ljava/lang/Object;)Ljava/lang/String;", false)

    // ── Switch ────────────────────────────────────────────────────────────────

    case SwitchExpr(target, cases, pos) =>
      compileExpr(target)
      compileArgArray(cases.flatMap(c => c.labels.map(l => l.getOrElse(NullLit(pos))) ++ List(c.body)))
      mv.visitVarInsn(ALOAD, 0)
      mv.visitLdcInsn(pos.file); mv.visitLdcInsn(pos.line); mv.visitLdcInsn(pos.column)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "execSwitch",
        "(Ljava/lang/Object;[Ljava/lang/Object;L" + CTX_CLS + ";Ljava/lang/String;II)Ljava/lang/Object;", false)

    // ── Try / catch / finally ─────────────────────────────────────────────────

    case TryExpr(body, catches, fin, _) =>
      val startLabel = new Label; val endLabel = new Label
      val finallyStart = new Label; val afterFinally = new Label
      mv.visitLabel(startLabel)
      compileExpr(body)
      val resultSlot = nextSlot.getAndIncrement()
      mv.visitVarInsn(ASTORE, resultSlot)
      // finally block (normal path)
      fin.foreach(compileStmt)
      mv.visitVarInsn(ALOAD, resultSlot)
      mv.visitJumpInsn(GOTO, afterFinally)
      // catch handlers
      val exSlot = nextSlot.getAndIncrement()
      for c <- catches do
        val handlerLabel = new Label
        mv.visitLabel(handlerLabel)
        mv.visitVarInsn(ASTORE, exSlot)
        // bind exception variable
        mv.visitVarInsn(ALOAD, 0)  // ctx
        mv.visitLdcInsn(c.varName)
        mv.visitVarInsn(ALOAD, exSlot)
        mv.visitMethodInsn(INVOKEVIRTUAL, CTX_CLS, "setValue",
          "(Ljava/lang/String;Ljava/lang/Object;)V", false)
        compileExpr(c.body)
        mv.visitVarInsn(ASTORE, resultSlot)
        fin.foreach(compileStmt)
        mv.visitVarInsn(ALOAD, resultSlot)
        mv.visitJumpInsn(GOTO, afterFinally)
        // register exception handler
        mv.visitTryCatchBlock(startLabel, handlerLabel, handlerLabel,
          c.typeName.mkString("/"))
      mv.visitLabel(endLabel)
      // finally on exception
      fin.foreach { f =>
        val excSlot = nextSlot.getAndIncrement()
        val excHandler = new Label
        mv.visitLabel(excHandler)
        mv.visitVarInsn(ASTORE, excSlot)
        compileStmt(f)
        mv.visitVarInsn(ALOAD, excSlot)
        mv.visitInsn(ATHROW)
        mv.visitTryCatchBlock(startLabel, endLabel, excHandler, null)
      }
      mv.visitLabel(afterFinally)

    // ── Package / Import ──────────────────────────────────────────────────────

    case ImportExpr(parts, wildcard, isStatic, dynamic, pos) =>
      mv.visitVarInsn(ALOAD, 0)  // ctx
      val importStr = if wildcard then parts.mkString(".") + ".*"
                      else parts.mkString(".")
      mv.visitLdcInsn(importStr)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "addImport",
        "(L" + CTX_CLS + ";Ljava/lang/String;)V", false)
      mv.visitInsn(ACONST_NULL)

    case PackageExpr(parts, dynamic, _) =>
      mv.visitVarInsn(ALOAD, 0)
      mv.visitLdcInsn(parts.mkString("."))
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "setPackage",
        "(L" + CTX_CLS + ";Ljava/lang/String;)V", false)
      mv.visitInsn(ACONST_NULL)

    case RecordDef(name, fields, _) =>
      mv.visitVarInsn(ALOAD, 0)
      mv.visitLdcInsn(name)
      mv.visitLdcInsn(fields.length)
      mv.visitTypeInsn(ANEWARRAY, "java/lang/String")
      for (f, i) <- fields.zipWithIndex do
        mv.visitInsn(DUP)
        mv.visitLdcInsn(i)
        mv.visitLdcInsn(f.fieldName)
        mv.visitInsn(AASTORE)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "defineRecord",
        "(L" + CTX_CLS + ";Ljava/lang/String;[Ljava/lang/String;)V", false)
      mv.visitInsn(ACONST_NULL)

    case ClassRef(name, pos) =>
      mv.visitVarInsn(ALOAD, 0)
      mv.visitLdcInsn(name.mkString("."))
      mv.visitLdcInsn(pos.file); mv.visitLdcInsn(pos.line); mv.visitLdcInsn(pos.column)
      mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "resolveClass",
        "(L" + CTX_CLS + ";Ljava/lang/String;Ljava/lang/String;II)Ljava/lang/Object;", false)

    case e =>
      // Fallback: throw UnsupportedOperationException at compile time
      throw new UnsupportedOperationException(s"CodeGen: unsupported AST node ${e.getClass.getSimpleName}")

  // ── Private helpers ────────────────────────────────────────────────────────

  private def compileExprList(exprs: List[Expr]): Unit =
    if exprs.isEmpty then mv.visitInsn(ACONST_NULL)
    else
      for e <- exprs.init do compileStmt(e)
      compileExpr(exprs.last)

  def compileStmt(expr: Expr): Unit =
    compileExpr(expr)
    mv.visitInsn(POP)

  /** Call Operators method returning Object (arithmetic ops). */
  private def callOperators(methodName: String, argc: Int): Unit =
    val desc = argc match
      case 1 => "(Ljava/lang/Object;)Ljava/lang/Object;"
      case 2 => "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
    mv.visitFieldInsn(GETSTATIC, OPS_CLS + "$", "MODULE$", "L" + OPS_CLS + "$;")
    mv.visitInsn(if argc == 2 then DUP_X2 else DUP_X1)
    mv.visitInsn(POP)
    mv.visitMethodInsn(INVOKEVIRTUAL, OPS_CLS + "$", methodName, desc, false)

  /** Call Operators comparison method returning primitive Z (boolean). */
  private def callOperatorsBool(methodName: String, argc: Int): Unit =
    val desc = argc match
      case 1 => "(Ljava/lang/Object;)Z"
      case 2 => "(Ljava/lang/Object;Ljava/lang/Object;)Z"
    mv.visitFieldInsn(GETSTATIC, OPS_CLS + "$", "MODULE$", "L" + OPS_CLS + "$;")
    mv.visitInsn(if argc == 2 then DUP_X2 else DUP_X1)
    mv.visitInsn(POP)
    mv.visitMethodInsn(INVOKEVIRTUAL, OPS_CLS + "$", methodName, desc, false)

  /** Call Operators comparison method and box the Z result to Boolean. */
  private def callOperatorsBoolBoxed(methodName: String, argc: Int): Unit =
    callOperatorsBool(methodName, argc)
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)

  private def callOperatorsToBoolean(): Unit =
    mv.visitFieldInsn(GETSTATIC, OPS_CLS + "$", "MODULE$", "L" + OPS_CLS + "$;")
    mv.visitInsn(SWAP)
    mv.visitMethodInsn(INVOKEVIRTUAL, OPS_CLS + "$", "toBoolean", "(Ljava/lang/Object;)Z", false)

  private def compileShortCircuit(lhs: Expr, rhs: Expr, isAnd: Boolean): Unit =
    val shortCircuit = new Label; val end = new Label
    compileExpr(lhs)
    callOperatorsToBoolean()
    if isAnd then
      mv.visitJumpInsn(IFEQ, shortCircuit)  // if false, short-circuit → false
      compileExpr(rhs)
      callOperatorsToBoolean()
      mv.visitJumpInsn(GOTO, end)
      mv.visitLabel(shortCircuit)
      mv.visitInsn(ICONST_0)
    else
      mv.visitJumpInsn(IFNE, shortCircuit)  // if true, short-circuit → true
      compileExpr(rhs)
      callOperatorsToBoolean()
      mv.visitJumpInsn(GOTO, end)
      mv.visitLabel(shortCircuit)
      mv.visitInsn(ICONST_1)
    mv.visitLabel(end)
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)

  private def compileIncr(operand: Expr, delta: Long, returnOld: Boolean): Unit =
    val deltaObj: Long = delta
    if returnOld then
      // Post-increment: return old, store new
      // Stack: [old, old, delta] → [old, new] → store new, leave old
      compileExpr(operand)                        // [old]
      mv.visitInsn(DUP)                           // [old, old]
      mv.visitLdcInsn(deltaObj)
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)
      callOperators("add", 2)                     // [old, new]
      storeToLhsVoid(operand)                     // store new, pop it → [old]
    else
      // Pre-increment: compute new, store it, return new
      compileExpr(operand)                        // [old]
      mv.visitLdcInsn(deltaObj)
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)
      callOperators("add", 2)                     // [new]
      storeToLhs(operand)                         // dup + store → [new]

  /** Store top-of-stack to LHS and consume it (no value left on stack). */
  private def storeToLhsVoid(lhs: Expr): Unit =
    lhs match
      case Ident(name, _) => storeVar(name, isVoid = true)
      case _ => storeToLhs(lhs); mv.visitInsn(POP)

  private def storeToLhs(lhs: Expr): Unit =
    lhs match
      case Ident(name, _)     => storeVar(name, isVoid = false)
      case GlobalRef(name, _) =>
        val valSlot = nextSlot.getAndIncrement()
        mv.visitVarInsn(ASTORE, valSlot)
        mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "setGlobal",
          "(Ljava/lang/String;Ljava/lang/Object;)V", false)
        mv.visitVarInsn(ALOAD, valSlot)
      case IndexAccess(obj, idx, pos) =>
        val valSlot = nextSlot.getAndIncrement()
        mv.visitVarInsn(ASTORE, valSlot)
        compileExpr(obj)
        compileExpr(idx)
        mv.visitVarInsn(ALOAD, valSlot)
        mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "setElement",
          "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false)
        mv.visitVarInsn(ALOAD, valSlot)
      case MemberAccess(obj, member, pos) =>
        val valSlot = nextSlot.getAndIncrement()
        mv.visitVarInsn(ASTORE, valSlot)
        compileExpr(obj)
        mv.visitLdcInsn(member)
        mv.visitVarInsn(ALOAD, valSlot)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitLdcInsn(pos.file); mv.visitLdcInsn(pos.line); mv.visitLdcInsn(pos.column)
        mv.visitMethodInsn(INVOKESTATIC, HELPER_CLS, "setField",
          "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;L" + CTX_CLS + ";Ljava/lang/String;II)V", false)
        mv.visitVarInsn(ALOAD, valSlot)
      case _ =>
        mv.visitInsn(POP)  // can't store, discard

  /** Store top-of-stack to variable `name`.
   *  If isVoid=false, leaves the value on the stack after storing (by dup-then-store). */
  private def storeVar(name: String, isVoid: Boolean): Unit =
    scope.slotOf(name) match
      case Some(slot) =>
        if !isVoid then mv.visitInsn(DUP)
        mv.visitVarInsn(ASTORE, slot + slotBase)
      case None =>
        // Extra local or context variable
        if scope.locals.contains(name) then
          val slot = extraLocals.getOrElseUpdate(name, nextSlot.getAndIncrement())
          if !isVoid then mv.visitInsn(DUP)
          mv.visitVarInsn(ASTORE, slot)
        else
          // Context variable
          if !isVoid then mv.visitInsn(DUP)
          val valSlot = nextSlot.getAndIncrement()
          mv.visitVarInsn(ASTORE, valSlot)
          mv.visitVarInsn(ALOAD, 0)  // ctx
          mv.visitLdcInsn(name)
          mv.visitVarInsn(ALOAD, valSlot)
          mv.visitMethodInsn(INVOKEVIRTUAL, CTX_CLS, "setValue",
            "(Ljava/lang/String;Ljava/lang/Object;)V", false)
          if !isVoid then mv.visitVarInsn(ALOAD, valSlot)

  private def compileArgArray(args: List[Expr]): Unit =
    mv.visitLdcInsn(args.length)
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object")
    for (a, i) <- args.zipWithIndex do
      mv.visitInsn(DUP)
      mv.visitLdcInsn(i)
      compileExpr(a)
      mv.visitInsn(AASTORE)

object CodeGen:
  val CTX_CLS     = "spnuts/runtime/Context"
  val PKG_CLS     = "spnuts/runtime/PnutsPackage"
  val OPS_CLS     = "spnuts/runtime/Operators"
  val HELPER_CLS  = "spnuts/compiler/CompiledHelper"
