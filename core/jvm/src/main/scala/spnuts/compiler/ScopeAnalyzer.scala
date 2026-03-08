package spnuts.compiler

import spnuts.ast.*

/**
 * Pre-pass scope analysis for the JIT compiler.
 *
 * For a given function body, identifies:
 *  - `params`       – function parameters (will occupy JVM local slots)
 *  - `localWrites`  – variables written inside this function body
 *  - `capturedBy`   – names that appear free in any nested FuncDef / closure
 *
 * If a name appears in `capturedBy` it must live in the Context (not a JVM slot),
 * because inner closures need to access it after the current activation.
 *
 * Everything else that is written only inside this function (and not captured)
 * can live in a JVM local slot.
 */
object ScopeAnalyzer:

  case class ScopeInfo(
    params:    List[String],          // declared parameter names (in order)
    locals:    Set[String],           // variables that can safely use JVM slots
    usesCtx:   Set[String],           // variables that must go through Context
    hasClosure: Boolean,              // any nested FuncDef / closure inside body?
  ):
    /** Slot index for a parameter or local variable (0 = Context, 1..n = vars). */
    def slotOf(name: String): Option[Int] =
      val idx = params.indexOf(name)
      if idx >= 0 then Some(idx + 1) else None  // slot 0 = Context

  def analyze(params: List[String], body: Expr): ScopeInfo =
    val writes   = collection.mutable.HashSet.empty[String]
    val captured = collection.mutable.HashSet.empty[String]
    var hasClosure = false

    collectWrites(body, params.toSet, writes, captured, () => { hasClosure = true })

    // variables that are written but not captured can use JVM local slots
    // Also include params that are not captured
    val safeLocals = (params.toSet ++ writes) -- captured
    val ctxVars    = captured.toSet

    ScopeInfo(params, safeLocals, ctxVars, hasClosure)

  private def collectWrites(
    expr:     Expr,
    outerParams: Set[String],
    writes:   collection.mutable.HashSet[String],
    captured: collection.mutable.HashSet[String],
    hasClosure: () => Unit,
  ): Unit =
    expr match
      case Assignment(_, Ident(name, _), rhs, _) =>
        writes += name
        collectWrites(rhs, outerParams, writes, captured, hasClosure)

      case MultiAssign(targets, rhs, _) =>
        targets.foreach(t => writes += t.name)
        collectWrites(rhs, outerParams, writes, captured, hasClosure)

      case FuncDef(_, innerParams, _, innerBody, _, _, _, _) =>
        // Names referenced in this closure that belong to the outer scope
        hasClosure()
        val frees = freeVarsOf(innerBody, innerParams.toSet)
        captured ++= frees.intersect(outerParams ++ writes)

      case Block(exprs, _)   => exprs.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
      case ExprList(exprs, _)=> exprs.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
      case BinaryExpr(_, l, r, _)  => collectWrites(l, outerParams, writes, captured, hasClosure); collectWrites(r, outerParams, writes, captured, hasClosure)
      case UnaryExpr(_, e, _)      => collectWrites(e, outerParams, writes, captured, hasClosure)
      case TernaryExpr(c, t, e, _) => Seq(c, t, e).foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
      case IfExpr(c, t, elseIfs, e, _) =>
        collectWrites(c, outerParams, writes, captured, hasClosure)
        collectWrites(t, outerParams, writes, captured, hasClosure)
        elseIfs.foreach { (ec, et) =>
          collectWrites(ec, outerParams, writes, captured, hasClosure)
          collectWrites(et, outerParams, writes, captured, hasClosure)
        }
        e.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
      case WhileExpr(c, b, _)     => collectWrites(c, outerParams, writes, captured, hasClosure); collectWrites(b, outerParams, writes, captured, hasClosure)
      case DoWhileExpr(b, c, _)   => collectWrites(b, outerParams, writes, captured, hasClosure); collectWrites(c, outerParams, writes, captured, hasClosure)
      case ForEachExpr(vs, it, b, _) =>
        vs.foreach(writes += _)
        collectWrites(it, outerParams, writes, captured, hasClosure)
        collectWrites(b, outerParams, writes, captured, hasClosure)
      case ForExpr(init, cond, update, body, _) =>
        init.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
        cond.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
        update.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
        collectWrites(body, outerParams, writes, captured, hasClosure)
      case ReturnExpr(v, _)       => v.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
      case BreakExpr(v, _)        => v.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
      case YieldExpr(v, _)        => v.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
      case ThrowExpr(v, _)        => v.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
      case FuncCall(f, as, _)     => collectWrites(f, outerParams, writes, captured, hasClosure); as.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
      case MethodCall(o, _, as, _) => collectWrites(o, outerParams, writes, captured, hasClosure); as.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
      case MemberAccess(o, _, _)  => collectWrites(o, outerParams, writes, captured, hasClosure)
      case IndexAccess(o, i, _)   => collectWrites(o, outerParams, writes, captured, hasClosure); collectWrites(i, outerParams, writes, captured, hasClosure)
      case ListExpr(es, _, _)     => es.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
      case MapExpr(pairs, _)      => pairs.foreach { (k, v) => collectWrites(k, outerParams, writes, captured, hasClosure); collectWrites(v, outerParams, writes, captured, hasClosure) }
      case TryExpr(b, cs, fin, _) =>
        collectWrites(b, outerParams, writes, captured, hasClosure)
        cs.foreach { c => writes += c.varName; collectWrites(c.body, outerParams, writes, captured, hasClosure) }
        fin.foreach(collectWrites(_, outerParams, writes, captured, hasClosure))
      case InterpolatedString(parts, _) =>
        parts.foreach {
          case Right(e) => collectWrites(e, outerParams, writes, captured, hasClosure)
          case Left(_)  => ()
        }
      case SwitchExpr(t, cs, _) =>
        collectWrites(t, outerParams, writes, captured, hasClosure)
        cs.foreach(c => collectWrites(c.body, outerParams, writes, captured, hasClosure))
      case _ => () // literals, Ident, etc. — no writes

  /** Collect all free variables referenced in `expr` not in `bound`. */
  private def freeVarsOf(expr: Expr, bound: Set[String]): Set[String] =
    val frees = collection.mutable.HashSet.empty[String]
    collectFreeRefs(expr, bound, frees)
    frees.toSet

  private def collectFreeRefs(
    expr: Expr, bound: Set[String], acc: collection.mutable.HashSet[String]
  ): Unit =
    expr match
      case Ident(name, _) if !bound(name) => acc += name
      case Assignment(_, Ident(name, _), rhs, _) =>
        collectFreeRefs(rhs, bound, acc)
      case FuncDef(_, ps, _, body, _, _, _, _) =>
        collectFreeRefs(body, bound ++ ps, acc)
      case Block(es, _)    => es.foreach(collectFreeRefs(_, bound, acc))
      case ExprList(es, _) => es.foreach(collectFreeRefs(_, bound, acc))
      case BinaryExpr(_, l, r, _) =>
        collectFreeRefs(l, bound, acc); collectFreeRefs(r, bound, acc)
      case UnaryExpr(_, e, _)         => collectFreeRefs(e, bound, acc)
      case TernaryExpr(c, t, e, _)    => Seq(c, t, e).foreach(collectFreeRefs(_, bound, acc))
      case IfExpr(c, t, eis, e, _)    =>
        collectFreeRefs(c, bound, acc); collectFreeRefs(t, bound, acc)
        eis.foreach { (ec, et) => collectFreeRefs(ec, bound, acc); collectFreeRefs(et, bound, acc) }
        e.foreach(collectFreeRefs(_, bound, acc))
      case WhileExpr(c, b, _)         => collectFreeRefs(c, bound, acc); collectFreeRefs(b, bound, acc)
      case FuncCall(f, as, _)         => collectFreeRefs(f, bound, acc); as.foreach(collectFreeRefs(_, bound, acc))
      case MethodCall(o, _, as, _)    => collectFreeRefs(o, bound, acc); as.foreach(collectFreeRefs(_, bound, acc))
      case MemberAccess(o, _, _)      => collectFreeRefs(o, bound, acc)
      case ReturnExpr(v, _)           => v.foreach(collectFreeRefs(_, bound, acc))
      case _ => ()
