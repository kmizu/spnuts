package spnuts.compiler

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spnuts.ast.{ClassDef, ClassDefBody, ExprList, SourcePos}
import spnuts.parser.Parser
import spnuts.runtime.PnutsPackage
import spnuts.typing.{StaticType, TypeError}

class CompilerTypingContractSpec extends AnyFlatSpec with Matchers:

  "Compiler.compileScript" should "initialize preflight in the supplied package" in {
    val packageName = "compiler_typing_contract"
    val script = Parser.parse(
      s"""x = 1
         |package compiler_typing_other
         |package $packageName
         |x = "bad"""".stripMargin,
      "<compiler-package>"
    ).asInstanceOf[ExprList]

    val error = intercept[TypeError] {
      Compiler.compileScript(script, PnutsPackage(packageName, None))
    }
    error.expected shouldBe Some(StaticType.LongType)
    error.actual shouldBe Some(StaticType.StringType)
  }

  it should "return None for unsupported code generation after successful preflight" in {
    val pos = SourcePos("<compiler-unsupported>", 1, 1)
    val script = ExprList(
      List(ClassDef("Unsupported", None, Nil, ClassDefBody(Nil, Nil), pos)),
      pos
    )

    Compiler.compileScript(
      script,
      PnutsPackage("compiler_unsupported_contract", None)
    ) shouldBe None
  }
