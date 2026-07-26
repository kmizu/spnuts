package spnuts.typing

import spnuts.ast.TypeExpr

sealed trait StaticType:
  def displayName: String

object StaticType:
  case object AnyType extends StaticType:
    val displayName = "Any"

  case object NullType extends StaticType:
    val displayName = "Null"

  case object UnitType extends StaticType:
    val displayName = "Unit"

  case object BooleanType extends StaticType:
    val displayName = "Boolean"

  case object LongType extends StaticType:
    val displayName = "Long"

  case object DoubleType extends StaticType:
    val displayName = "Double"

  case object CharType extends StaticType:
    val displayName = "Char"

  case object StringType extends StaticType:
    val displayName = "String"

  final case class ListType(element: StaticType) extends StaticType:
    def displayName: String = s"List<${element.displayName}>"

  final case class MapType(key: StaticType, value: StaticType) extends StaticType:
    def displayName: String = s"Map<${key.displayName}, ${value.displayName}>"

  final case class ArrayType(element: StaticType) extends StaticType:
    def displayName: String = s"${element.displayName}[]"

  final case class FunctionType(
    parameters: List[StaticType],
    result: StaticType,
    varargElement: Option[StaticType] = None
  ) extends StaticType:
    def displayName: String =
      val displayedParameters = parameters.map(_.displayName) ++ varargElement.map(tpe => s"${tpe.displayName}*")
      s"(${displayedParameters.mkString(", ")}) -> ${result.displayName}"

  final case class NamedType(name: String, arguments: List[StaticType] = Nil)
      extends StaticType:
    def displayName: String =
      if arguments.isEmpty then name
      else s"$name<${arguments.map(_.displayName).mkString(", ")}>"

  final case class TypeVariable(name: String) extends StaticType:
    val displayName = name

  def fromTypeExpr(expr: TypeExpr, typeVariables: Set[String] = Set.empty): StaticType =
    if TypeExpr.isArrayType(expr) then ArrayType(fromTypeExpr(TypeExpr.arrayElemType(expr), typeVariables))
    else if TypeExpr.isFuncType(expr) then
      FunctionType(
        TypeExpr.funcParams(expr).map(fromTypeExpr(_, typeVariables)),
        fromTypeExpr(TypeExpr.funcReturn(expr), typeVariables)
      )
    else if TypeExpr.isVarargFuncType(expr) then
      FunctionType(
        TypeExpr.varargFixedParams(expr).map(fromTypeExpr(_, typeVariables)),
        fromTypeExpr(TypeExpr.varargFuncReturn(expr), typeVariables),
        Some(fromTypeExpr(TypeExpr.varargElemType(expr), typeVariables))
      )
    else
      val name = expr.name.mkString(".")
      val arguments = expr.typeArgs.map(fromTypeExpr(_, typeVariables))
      name match
        case "?" | "Any" | "Object" | "java.lang.Object" => AnyType
        case "Null" => NullType
        case "Unit" | "void" | "scala.Unit" => UnitType
        case "Boolean" | "boolean" | "java.lang.Boolean" => BooleanType
        case "Byte" | "Short" | "Int" | "Integer" | "Long" |
            "byte" | "short" | "int" | "long" |
            "java.lang.Byte" | "java.lang.Short" | "java.lang.Integer" | "java.lang.Long" => LongType
        case "Float" | "Double" | "float" | "double" |
            "java.lang.Float" | "java.lang.Double" => DoubleType
        case "Char" | "char" | "java.lang.Character" => CharType
        case "String" | "java.lang.String" => StringType
        case "List" | "scala.collection.immutable.List" | "java.util.List" if arguments.length == 1 =>
          ListType(arguments.head)
        case "Map" | "scala.collection.immutable.Map" | "java.util.Map" if arguments.length == 2 =>
          MapType(arguments.head, arguments(1))
        case variable if expr.name.length == 1 && typeVariables.contains(variable) => TypeVariable(variable)
        case _ => NamedType(name, arguments)
