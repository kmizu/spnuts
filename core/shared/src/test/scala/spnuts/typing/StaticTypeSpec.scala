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

  "TypeRules.join" should "preserve useful common structure and otherwise use Any" in {
    TypeRules.join(LongType, DoubleType) shouldBe DoubleType
    TypeRules.join(StringType, NullType) shouldBe StringType
    TypeRules.join(ListType(LongType), ListType(DoubleType)) shouldBe
      ListType(DoubleType)
    TypeRules.join(CharType, LongType) shouldBe AnyType
    TypeRules.join(CharType, DoubleType) shouldBe AnyType
    TypeRules.join(LongType, StringType) shouldBe AnyType
  }
