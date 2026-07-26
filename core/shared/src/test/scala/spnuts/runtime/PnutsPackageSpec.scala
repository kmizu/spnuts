package spnuts.runtime

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PnutsPackageSpec extends AnyFlatSpec with Matchers:

  "PnutsPackage.allBindings" should "enumerate local bindings without parent bindings" in {
    val parent = PnutsPackage("parent")
    parent.set("inParent", 1)
    val child = PnutsPackage("child", Some(parent))
    child.set("local", 2)

    child.allBindings.map((name, binding) => name -> binding.value).toMap shouldBe Map("local" -> 2)
  }
