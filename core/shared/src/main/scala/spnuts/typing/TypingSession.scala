package spnuts.typing

final class TypingSession private (private var environment: TypeEnvironment):
  def snapshot: TypeEnvironment = environment

  def commit(next: TypeEnvironment): Unit = environment = next

object TypingSession:
  def apply(): TypingSession = new TypingSession(TypeEnvironment.empty)

final case class TypingResult(
  table: TypeTable,
  nextEnvironment: TypeEnvironment,
  resultType: StaticType
)
