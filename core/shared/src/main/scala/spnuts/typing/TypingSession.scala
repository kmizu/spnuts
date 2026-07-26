package spnuts.typing

final class TypingSession private (
  private var environment: TypeEnvironment,
  private var stages: List[TypeEnvironment]
):
  def snapshot: TypeEnvironment = stages.headOption.getOrElse(environment)

  def begin(next: TypeEnvironment): Unit =
    stages = next :: stages

  def commit(): Unit =
    stages match
      case current :: _ :: rest => stages = current :: rest
      case current :: Nil =>
        environment = current
        stages = Nil
      case Nil =>
        throw IllegalStateException("no typing transaction to commit")

  def rollback(): Unit =
    stages match
      case _ :: rest => stages = rest
      case Nil => throw IllegalStateException("no typing transaction to roll back")

  def commit(next: TypeEnvironment): Unit =
    stages match
      case _ :: rest => stages = next :: rest
      case Nil => environment = next

object TypingSession:
  def apply(): TypingSession = new TypingSession(TypeEnvironment.empty, Nil)

final case class TypingResult(
  table: TypeTable,
  nextEnvironment: TypeEnvironment,
  resultType: StaticType
)
