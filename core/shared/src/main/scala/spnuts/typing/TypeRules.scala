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
    (expected == DoubleType && actual == LongType) ||
    (actual == NullType && isReference(expected)) ||
    ((expected, actual) match
      case (ListType(expectedElement), ListType(actualElement)) =>
        isCompatible(expectedElement, actualElement)
      case (
            MapType(expectedKey, expectedValue),
            MapType(actualKey, actualValue)
          ) =>
        isCompatible(expectedKey, actualKey) &&
          isCompatible(expectedValue, actualValue)
      case (ArrayType(expectedElement), ArrayType(actualElement)) =>
        isCompatible(expectedElement, actualElement)
      case (
            FunctionType(expectedParameters, expectedResult, expectedVararg),
            FunctionType(actualParameters, actualResult, actualVararg)
          ) =>
        expectedParameters.length == actualParameters.length &&
          expectedVararg.isDefined == actualVararg.isDefined &&
          expectedParameters.zip(actualParameters).forall {
            (expectedParameter, actualParameter) =>
              isCompatible(actualParameter, expectedParameter)
          } &&
          isCompatible(expectedResult, actualResult) &&
          expectedVararg.zip(actualVararg).forall {
            (expectedElement, actualElement) =>
              isCompatible(actualElement, expectedElement)
          }
      case (
            NamedType(expectedName, expectedArguments),
            NamedType(actualName, actualArguments)
          ) =>
        expectedName == actualName &&
          expectedArguments.length == actualArguments.length &&
          expectedArguments.zip(actualArguments).forall(isCompatible)
      case _ => false)

  def join(left: StaticType, right: StaticType): StaticType =
    if left == right then left
    else (left, right) match
      case (AnyType, _) | (_, AnyType) => AnyType
      case (DoubleType, LongType) | (LongType, DoubleType) => DoubleType
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
