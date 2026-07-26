package spnuts.typing

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spnuts.ast.TypeExpr
import spnuts.typing.StaticType.*

class StaticTypeSpec extends AnyFlatSpec with Matchers:
  "StaticType.fromTypeExpr" should "normalize aliases without JVM reflection" in {
    StaticType.fromTypeExpr(TypeExpr(List("Int"))) shouldBe LongType
    StaticType.fromTypeExpr(TypeExpr(List("Float"))) shouldBe DoubleType
    StaticType.fromTypeExpr(TypeExpr(List("Any"))) shouldBe AnyType
    StaticType.fromTypeExpr(TypeExpr.Wildcard) shouldBe AnyType
    StaticType.fromTypeExpr(TypeExpr.array(TypeExpr(List("String")))) shouldBe
      ArrayType(StringType)
  }

  it should "retain collection, function, named, and type-variable structure" in {
    StaticType.fromTypeExpr(
      TypeExpr(List("List"), List(TypeExpr(List("String"))))
    ) shouldBe ListType(StringType)
    StaticType.fromTypeExpr(
      TypeExpr.func(List(TypeExpr(List("Long"))), TypeExpr(List("Boolean")))
    ) shouldBe FunctionType(List(LongType), BooleanType, None)
    StaticType.fromTypeExpr(
      TypeExpr(List("com", "acme", "Thing"), List(TypeExpr(List("String"))))
    ) shouldBe NamedType("com.acme.Thing", List(StringType))
    StaticType.fromTypeExpr(TypeExpr(List("T")), Set("T")) shouldBe TypeVariable("T")
  }

  "TypeRules.isCompatible" should "allow only the specified gradual and numeric cases" in {
    TypeRules.isCompatible(LongType, LongType) shouldBe true
    TypeRules.isCompatible(DoubleType, LongType) shouldBe true
    TypeRules.isCompatible(LongType, DoubleType) shouldBe false
    TypeRules.isCompatible(LongType, CharType) shouldBe false
    TypeRules.isCompatible(DoubleType, CharType) shouldBe false
    TypeRules.isCompatible(StringType, NullType) shouldBe true
    TypeRules.isCompatible(LongType, NullType) shouldBe false
    TypeRules.isCompatible(StringType, AnyType) shouldBe true
    TypeRules.isCompatible(AnyType, StringType) shouldBe true
  }

  it should "apply gradual compatibility recursively to structural types" in {
    TypeRules.isCompatible(ListType(AnyType), ListType(LongType)) shouldBe true
    TypeRules.isCompatible(ListType(LongType), ListType(AnyType)) shouldBe true
    TypeRules.isCompatible(ListType(DoubleType), ListType(LongType)) shouldBe true
    TypeRules.isCompatible(ListType(LongType), ListType(DoubleType)) shouldBe false
    TypeRules.isCompatible(ListType(LongType), ListType(CharType)) shouldBe false

    TypeRules.isCompatible(
      MapType(StringType, AnyType),
      MapType(StringType, LongType)
    ) shouldBe true
    TypeRules.isCompatible(
      MapType(StringType, LongType),
      MapType(LongType, LongType)
    ) shouldBe false
    TypeRules.isCompatible(ArrayType(AnyType), ArrayType(LongType)) shouldBe true

    TypeRules.isCompatible(
      FunctionType(List(LongType), LongType),
      FunctionType(List(AnyType), AnyType)
    ) shouldBe true
    TypeRules.isCompatible(
      FunctionType(List(StringType), LongType, Some(AnyType)),
      FunctionType(List(StringType), LongType, Some(LongType))
    ) shouldBe true
    TypeRules.isCompatible(
      FunctionType(List(LongType), LongType),
      FunctionType(List(LongType, LongType), LongType)
    ) shouldBe false
    TypeRules.isCompatible(
      FunctionType(List(LongType), LongType),
      FunctionType(List(LongType), LongType, Some(LongType))
    ) shouldBe false

    TypeRules.isCompatible(
      NamedType("Box", List(AnyType)),
      NamedType("Box", List(LongType))
    ) shouldBe true
    TypeRules.isCompatible(
      NamedType("Box", List(AnyType)),
      NamedType("Other", List(LongType))
    ) shouldBe false
  }

  "TypeRules.join" should "preserve useful common structure and otherwise use Any" in {
    TypeRules.join(LongType, DoubleType) shouldBe DoubleType
    TypeRules.join(StringType, NullType) shouldBe StringType
    TypeRules.join(ListType(LongType), ListType(DoubleType)) shouldBe
      ListType(DoubleType)
    TypeRules.join(CharType, LongType) shouldBe AnyType
    TypeRules.join(CharType, DoubleType) shouldBe AnyType
    TypeRules.join(LongType, StringType) shouldBe AnyType
  }
