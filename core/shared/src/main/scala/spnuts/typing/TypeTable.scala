package spnuts.typing

import spnuts.ast.Expr

final class TypeTable private ():
  private val entries = new java.util.IdentityHashMap[Expr, StaticType]()

  def record(expr: Expr, tpe: StaticType): StaticType =
    entries.put(expr, tpe)
    tpe

  def get(expr: Expr): Option[StaticType] = Option(entries.get(expr))

  def apply(expr: Expr): StaticType = entries.get(expr)

object TypeTable:
  def apply(): TypeTable = new TypeTable()
