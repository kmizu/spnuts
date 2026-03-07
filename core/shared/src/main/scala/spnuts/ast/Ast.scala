package spnuts.ast

import SourcePos.*

// ── Operator enumerations ─────────────────────────────────────────────────────

enum BinOp:
  case Add, Sub, Mul, Div, Mod
  case BitAnd, BitOr, BitXor
  case ShiftLeft, ShiftRight, UnsignedShiftRight
  case Eq, NotEq, Lt, Gt, Le, Ge
  case LogAnd, LogOr

enum UnaryOp:
  case Neg, BitNot, LogNot, PreIncr, PreDecr, PostIncr, PostDecr

enum AssignOp:
  case Assign
  case AddAssign, SubAssign, MulAssign, DivAssign, ModAssign
  case AndAssign, OrAssign, XorAssign
  case ShiftLeftAssign, ShiftRightAssign, UnsignedShiftRightAssign

// ── AST node base ─────────────────────────────────────────────────────────────

sealed trait Expr:
  def pos: SourcePos

// ── Literals ──────────────────────────────────────────────────────────────────

/** Integer literal. value is Int, Long, or BigInt. */
case class IntLit(value: Any, raw: String, pos: SourcePos) extends Expr
case class FloatLit(value: Any, raw: String, pos: SourcePos) extends Expr
case class CharLit(value: Char, pos: SourcePos) extends Expr
case class StringLit(value: String, pos: SourcePos) extends Expr

/** String interpolation: "Hello \(name)!" -> parts = [Left("Hello "), Right(Ident("name")), Left("!")] */
case class InterpolatedString(parts: List[Either[String, Expr]], pos: SourcePos) extends Expr
case class BoolLit(value: Boolean, pos: SourcePos) extends Expr
case class NullLit(pos: SourcePos) extends Expr

// ── Identifiers ───────────────────────────────────────────────────────────────

/** Local or package variable reference. */
case class Ident(name: String, pos: SourcePos) extends Expr

/** Global package reference  ::name */
case class GlobalRef(name: String, pos: SourcePos) extends Expr

// ── Operators ─────────────────────────────────────────────────────────────────

case class BinaryExpr(op: BinOp, lhs: Expr, rhs: Expr, pos: SourcePos) extends Expr
case class UnaryExpr(op: UnaryOp, operand: Expr, pos: SourcePos) extends Expr
case class TernaryExpr(cond: Expr, thenExpr: Expr, elseExpr: Expr, pos: SourcePos) extends Expr
case class InstanceofExpr(expr: Expr, typeName: List[String], pos: SourcePos) extends Expr

// ── Assignment ────────────────────────────────────────────────────────────────

case class Assignment(op: AssignOp, lhs: Expr, rhs: Expr, pos: SourcePos) extends Expr

/** a, b = expr */
case class MultiAssign(targets: List[Ident], rhs: Expr, pos: SourcePos) extends Expr

// ── Member access ─────────────────────────────────────────────────────────────

case class MemberAccess(obj: Expr, member: String, pos: SourcePos) extends Expr
case class StaticMemberAccess(obj: Expr, member: String, pos: SourcePos) extends Expr
case class MethodCall(obj: Expr, method: String, args: List[Expr], pos: SourcePos) extends Expr
case class StaticMethodCall(obj: Expr, method: String, args: List[Expr], pos: SourcePos) extends Expr
case class FuncCall(func: Expr, args: List[Expr], pos: SourcePos) extends Expr
case class IndexAccess(obj: Expr, index: Expr, pos: SourcePos) extends Expr

/** arr[from..to] or arr[from..] */
case class RangeAccess(obj: Expr, from: Expr, to: Option[Expr], pos: SourcePos) extends Expr

/** from..to  (inclusive range, used as iterable) */
case class RangeExpr(from: Expr, to: Expr, pos: SourcePos) extends Expr

// ── Collections ───────────────────────────────────────────────────────────────

/** [e1, e2, ...] or {e1, e2, ...} (braced=true) */
case class ListExpr(elements: List[Expr], braced: Boolean, pos: SourcePos) extends Expr

/** { k1 => v1, k2 => v2, ... } */
case class MapExpr(entries: List[(Expr, Expr)], pos: SourcePos) extends Expr

// ── Control flow ──────────────────────────────────────────────────────────────

case class IfExpr(
  cond: Expr,
  thenBranch: Expr,
  elseIfs: List[(Expr, Expr)],
  elseBranch: Option[Expr],
  pos: SourcePos
) extends Expr

case class WhileExpr(cond: Expr, body: Expr, pos: SourcePos) extends Expr
case class DoWhileExpr(body: Expr, cond: Expr, pos: SourcePos) extends Expr

/** C-style: for (init; cond; update) body */
case class ForExpr(
  init: Option[Expr],
  cond: Option[Expr],
  update: Option[Expr],
  body: Expr,
  pos: SourcePos
) extends Expr

/** for (vars : iterable) body  or  for (var : start .. end) body */
case class ForEachExpr(vars: List[String], iterable: Expr, body: Expr, pos: SourcePos) extends Expr

/** foreach var [list] body  or  foreach var (expr) body */
case class ForeachExpr(varName: String, iterable: Expr, body: Expr, pos: SourcePos) extends Expr

case class SwitchExpr(target: Expr, cases: List[SwitchCase], pos: SourcePos) extends Expr
case class SwitchCase(labels: List[Option[Expr]], body: Expr) // None = default

// ── Functions ─────────────────────────────────────────────────────────────────

/** function [name](params [[]]) body  or  closure  { params -> body } */
case class FuncDef(
  name: Option[String],
  params: List[String],
  varargs: Boolean,
  body: Expr,
  pos: SourcePos
) extends Expr

case class ReturnExpr(value: Option[Expr], pos: SourcePos) extends Expr
case class YieldExpr(value: Option[Expr], pos: SourcePos) extends Expr
case class BreakExpr(value: Option[Expr], pos: SourcePos) extends Expr
case class ContinueExpr(pos: SourcePos) extends Expr

// ── Exception handling ────────────────────────────────────────────────────────

case class TryExpr(body: Expr, catches: List[CatchClause], finallyBlock: Option[Expr], pos: SourcePos) extends Expr
case class CatchClause(typeName: List[String], varName: String, body: Expr, pos: SourcePos)
case class ThrowExpr(expr: Option[Expr], pos: SourcePos) extends Expr

/** Standalone catch(cls, handler) — functional style */
case class CatchExpr(cls: Expr, handler: Expr, pos: SourcePos) extends Expr

/** Standalone finally(body) or finally(body, finalizer) */
case class FinallyExpr(body: Expr, finalizer: Option[Expr], pos: SourcePos) extends Expr

// ── Class / Type (JVM-centric, restricted on Native) ─────────────────────────

case class ClassDef(
  name: String,
  superClass: Option[List[String]],
  interfaces: List[List[String]],
  body: ClassDefBody,
  pos: SourcePos
) extends Expr

case class ClassDefBody(fields: List[FieldDef], methods: List[MethodDef])
case class FieldDef(typeName: Option[List[String]], name: String, init: Option[Expr])
case class MethodDef(returnType: Option[List[String]], name: String, params: List[(Option[List[String]], String)], body: Expr)

case class NewExpr(className: List[String], dims: List[Expr], args: List[Expr], classBody: Option[ClassDefBody], pos: SourcePos) extends Expr
case class CastExpr(typeName: List[String], arrayDims: Int, expr: Expr, pos: SourcePos) extends Expr

/** class(expr) — dynamic class lookup */
case class ClassExpr(expr: Expr, pos: SourcePos) extends Expr

/** class ClassName — class reference */
case class ClassRef(name: List[String], pos: SourcePos) extends Expr

/** JavaBeans-style construction: ClassName { prop: val; ... } */
case class BeanDef(typeName: List[String], props: List[BeanProperty], pos: SourcePos) extends Expr
case class BeanProperty(name: String, value: Expr, useStatic: Boolean) // :: = static bind

// ── Package / Import ──────────────────────────────────────────────────────────

case class PackageExpr(parts: List[String], dynamic: Option[Expr], pos: SourcePos) extends Expr
case class ImportExpr(parts: List[String], wildcard: Boolean, isStatic: Boolean, dynamic: Option[Expr], pos: SourcePos) extends Expr

// ── Block / Program ───────────────────────────────────────────────────────────

/** { e1; e2; ... } */
case class Block(exprs: List[Expr], pos: SourcePos) extends Expr

/** e1; e2; ... (top-level expression list) */
case class ExprList(exprs: List[Expr], pos: SourcePos) extends Expr
