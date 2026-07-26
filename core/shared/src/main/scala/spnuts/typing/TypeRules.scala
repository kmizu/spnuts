package spnuts.typing

import spnuts.typing.StaticType.*

object TypeRules:
  def isReference(tpe: StaticType): Boolean = tpe match
    case AnyType | NullType | StringType | ListType(_) | MapType(_, _) | ArrayType(_) |
        FunctionType(_, _, _) | NamedType(_, _) | TypeVariable(_) => true
    case UnitType | BooleanType | LongType | DoubleType | CharType => false

  def isCompatible(expected: StaticType, actual: StaticType): Boolean =
    expected == actual ||
    expected == AnyType || actual == AnyType ||
    (expected == DoubleType && (actual == LongType || actual == CharType)) ||
    (expected == LongType && actual == CharType) ||
    (actual == NullType && isReference(expected))

  def join(left: StaticType, right: StaticType): StaticType =
    if left == right then left
    else (left, right) match
      case (AnyType, _) | (_, AnyType) => AnyType
      case (DoubleType, LongType | CharType) | (LongType | CharType, DoubleType) => DoubleType
      case (LongType, CharType) | (CharType, LongType) => LongType
      case (NullType, reference) if isReference(reference) => reference
      case (reference, NullType) if isReference(reference) => reference
      case (ListType(leftElement), ListType(rightElement)) => ListType(join(leftElement, rightElement))
      case (MapType(leftKey, leftValue), MapType(rightKey, rightValue)) =>
        MapType(join(leftKey, rightKey), join(leftValue, rightValue))
      case (ArrayType(leftElement), ArrayType(rightElement)) => ArrayType(join(leftElement, rightElement))
      case (NamedType(leftName, leftArguments), NamedType(rightName, rightArguments))
          if leftName == rightName && leftArguments.length == rightArguments.length =>
        NamedType(leftName, leftArguments.zip(rightArguments).map(join))
      case (
            FunctionType(leftParameters, leftResult, leftVararg),
            FunctionType(rightParameters, rightResult, rightVararg)
          ) if leftParameters.length == rightParameters.length && leftVararg.isDefined == rightVararg.isDefined =>
        FunctionType(
          leftParameters.zip(rightParameters).map(join),
          join(leftResult, rightResult),
          leftVararg.zip(rightVararg).headOption.map(join)
        )
      case _ => AnyType

  def joinAll(types: Iterable[StaticType], ifEmpty: StaticType): StaticType =
    types.iterator.reduceOption(join).getOrElse(ifEmpty)
