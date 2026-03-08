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

enum DeclKind:
  case Val, Var

// ── Type expressions ──────────────────────────────────────────────────────────

/**
 * A type reference, possibly parameterized, or a function type.
 *
 * Regular types:
 *   - `String`              → TypeExpr(List("String"), Nil)
 *   - `java.util.List`      → TypeExpr(List("java","util","List"), Nil)
 *   - `List<String>`        → TypeExpr(List("List"), List(TypeExpr(List("String"), Nil)))
 *   - `Map<String,Integer>` → TypeExpr(List("Map"), List(..., ...))
 *   - `T` (type variable)   → TypeExpr(List("T"), Nil) — resolved via typeParams bindings
 *   - `?` (wildcard)        → TypeExpr(List("?"), Nil)
 *
 * Array types — encoded with name == List("[]"):
 *   - `Long[]`              → TypeExpr(List("[]"), List(TypeExpr(List("Long"), Nil)))
 *   - `String[][]`          → TypeExpr(List("[]"), List(TypeExpr(List("[]"), List(StringTE))))
 *
 * Fixed-arity function type — encoded with name == List("->"):
 *   - `(Long, String) -> Boolean`  → TypeExpr(List("->"), List(LongTE, StringTE, BoolTE))
 *   - `() -> Long`                 → TypeExpr(List("->"), List(LongTE))
 *   Convention: typeArgs = fixedParamTypes :+ returnType
 *
 * Varargs function type — encoded with name == List("->*"):
 *   - `(Long*) -> Long`            → TypeExpr(List("->*"), List(LongTE, LongTE))
 *   - `(String, Long*) -> Long`    → TypeExpr(List("->*"), List(StringTE, LongTE, LongTE))
 *   Convention: typeArgs = fixedParamTypes :+ varargElemType :+ returnType
 *   (last element = returnType, second-to-last = vararg element type, rest = fixed params)
 */
case class TypeExpr(name: List[String], typeArgs: List[TypeExpr] = Nil):
  def toDisplayString: String =
    if TypeExpr.isArrayType(this) then
      TypeExpr.arrayElemType(this).toDisplayString + "[]"
    else if TypeExpr.isFuncType(this) then
      val params = TypeExpr.funcParams(this).map(_.toDisplayString).mkString(", ")
      val ret    = TypeExpr.funcReturn(this).toDisplayString
      s"($params) -> $ret"
    else if TypeExpr.isVarargFuncType(this) then
      val fixed  = TypeExpr.varargFixedParams(this).map(_.toDisplayString)
      val varg   = TypeExpr.varargElemType(this).toDisplayString + "*"
      val ret    = TypeExpr.varargFuncReturn(this).toDisplayString
      s"(${(fixed :+ varg).mkString(", ")}) -> $ret"
    else
      val base = name.mkString(".")
      if typeArgs.isEmpty then base
      else s"$base<${typeArgs.map(_.toDisplayString).mkString(", ")}>"

object TypeExpr:
  /** Wildcard type argument `?` — erases to Object at runtime. */
  val Wildcard: TypeExpr = TypeExpr(List("?"), Nil)

  // ── Array types `A[]` ──────────────────────────────────────────────────────

  /** Construct an array type `elemType[]`. */
  def array(elemType: TypeExpr): TypeExpr = TypeExpr(List("[]"), List(elemType))

  /** True if this is an array type. */
  def isArrayType(te: TypeExpr): Boolean = te.name == List("[]") && te.typeArgs.length == 1

  /** Element type of an array type. */
  def arrayElemType(te: TypeExpr): TypeExpr = te.typeArgs.head

  // ── Fixed-arity function types ─────────────────────────────────────────────

  /** Construct a fixed-arity function type `(paramTypes...) -> returnType`. */
  def func(paramTypes: List[TypeExpr], returnType: TypeExpr): TypeExpr =
    TypeExpr(List("->"), paramTypes :+ returnType)

  /** True if this is a fixed-arity function type. */
  def isFuncType(te: TypeExpr): Boolean =
    te.name == List("->") && te.typeArgs.nonEmpty

  /** Fixed parameter types of a function type. */
  def funcParams(te: TypeExpr): List[TypeExpr] = te.typeArgs.dropRight(1)

  /** Return type of a function type. */
  def funcReturn(te: TypeExpr): TypeExpr = te.typeArgs.last

  // ── Varargs function types `(A, B*) -> C` ─────────────────────────────────

  /**
   * Construct a varargs function type `(fixedParams..., varargElem*) -> returnType`.
   * E.g. `(String, Long*) -> Long` = funcVararg(List(StringTE), LongTE, LongTE)
   */
  def funcVararg(fixedParams: List[TypeExpr], varargElem: TypeExpr, returnType: TypeExpr): TypeExpr =
    TypeExpr(List("->*"), fixedParams ++ List(varargElem, returnType))

  /** True if this is a varargs function type. */
  def isVarargFuncType(te: TypeExpr): Boolean =
    te.name == List("->*") && te.typeArgs.length >= 2

  /** Fixed (non-vararg) parameter types of a varargs function type. */
  def varargFixedParams(te: TypeExpr): List[TypeExpr] = te.typeArgs.dropRight(2)

  /** The element type of the vararg parameter. */
  def varargElemType(te: TypeExpr): TypeExpr = te.typeArgs(te.typeArgs.length - 2)

  /** Return type of a varargs function type. */
  def varargFuncReturn(te: TypeExpr): TypeExpr = te.typeArgs.last

  /** Minimum arity for a varargs function type (= number of fixed params). */
  def varargMinArity(te: TypeExpr): Int = te.typeArgs.length - 2

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

/** `val name [: Type] = expr` (immutable) or `var name [: Type] = expr` (mutable). */
case class VarDecl(kind: DeclKind, name: String, typeName: Option[TypeExpr], value: Expr, pos: SourcePos) extends Expr

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
  pos: SourcePos,
  /** Generic type parameter names, e.g. List("T","U") for `<T, U>`. */
  typeParams: List[String] = Nil,
  /** Optional type annotation for each parameter, parallel to `params`. */
  paramTypes: List[Option[TypeExpr]] = Nil,
  /** Optional return type annotation. */
  returnType: Option[TypeExpr] = None
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

// ── Record type (works on both JVM and Native) ───────────────────────────────

/** `record Name(type field, ...)` — immutable value type with named fields */
case class RecordDef(
  name: String,
  fields: List[RecordField],
  pos: SourcePos
) extends Expr

case class RecordField(typeName: Option[String], fieldName: String)

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
