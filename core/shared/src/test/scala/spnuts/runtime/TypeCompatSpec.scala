package spnuts.runtime

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.Inspectors.forEvery
import org.scalatest.matchers.should.Matchers

class TypeCompatSpec extends AnyFlatSpec with Matchers:

  "TypeCompat" should "reject null for primitive semantic aliases" in {
    val primitiveAliases = List[Class[?]](
      classOf[java.lang.Byte],
      classOf[java.lang.Short],
      classOf[java.lang.Integer],
      classOf[java.lang.Long],
      classOf[java.lang.Float],
      classOf[java.lang.Double],
      classOf[java.lang.Character],
      classOf[java.lang.Boolean]
    )

    forEvery(primitiveAliases) { alias =>
      TypeCompat.isCompatible(alias, null) shouldBe false
    }
  }

  it should "accept null for references and preserve Unit compatibility" in {
    TypeCompat.isCompatible(classOf[String], null) shouldBe true
    TypeCompat.isCompatible(classOf[Object], null) shouldBe true
    TypeCompat.isCompatible(classOf[scala.runtime.BoxedUnit], null) shouldBe true
    TypeCompat.isCompatible(classOf[scala.runtime.BoxedUnit], 1L) shouldBe true
  }
