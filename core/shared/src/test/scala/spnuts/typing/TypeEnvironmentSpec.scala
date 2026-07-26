package spnuts.typing

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spnuts.ast.{IntLit, SourcePos}
import spnuts.typing.StaticType.*

class TypeEnvironmentSpec extends AnyFlatSpec with Matchers:
  "TypeEnvironment" should "look up inner scopes before outer scopes" in {
    val outer = TypeEnvironment.empty.declare("x", TypeBinding(LongType, false))
    val inner = outer.pushScope.declare("x", TypeBinding(StringType, true))
    inner.lookup("x") shouldBe Some(TypeBinding(StringType, true))
    inner.popScope.lookup("x") shouldBe Some(TypeBinding(LongType, false))
  }

  "TypingSession" should "change only after an explicit commit" in {
    val session = TypingSession()
    val next = session.snapshot.declare("x", TypeBinding(LongType, false))
    session.snapshot.lookup("x") shouldBe None
    session.commit(next)
    session.snapshot.lookup("x") shouldBe Some(TypeBinding(LongType, false))
  }

  it should "merge a successful nested stage into its parent before publishing" in {
    val session = TypingSession()
    val outer = session.snapshot.declare("x", TypeBinding(LongType, false))
    session.begin(outer)
    val inner = session.snapshot.declare("y", TypeBinding(StringType, false))
    session.begin(inner)

    session.commit()
    session.snapshot.lookup("x") shouldBe Some(TypeBinding(LongType, false))
    session.snapshot.lookup("y") shouldBe Some(TypeBinding(StringType, false))

    session.commit()
    session.snapshot.lookup("x") shouldBe Some(TypeBinding(LongType, false))
    session.snapshot.lookup("y") shouldBe Some(TypeBinding(StringType, false))
  }

  it should "discard merged nested stages when the outer stage rolls back" in {
    val session = TypingSession()
    val outer = session.snapshot.declare("x", TypeBinding(LongType, false))
    session.begin(outer)
    val inner = session.snapshot.declare("y", TypeBinding(StringType, false))
    session.begin(inner)

    session.commit()
    session.rollback()

    session.snapshot.lookup("x") shouldBe None
    session.snapshot.lookup("y") shouldBe None
  }

  "TypeTable" should "distinguish structurally equal expression instances" in {
    val p = SourcePos("<test>", 1, 1)
    val a = IntLit(1L, "1", p)
    val b = IntLit(1L, "1", p)
    val table = TypeTable()
    table.record(a, LongType)
    table.record(b, DoubleType)
    table(a) shouldBe LongType
    table(b) shouldBe DoubleType
  }
