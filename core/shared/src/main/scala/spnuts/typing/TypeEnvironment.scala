package spnuts.typing

final case class TypeBinding(tpe: StaticType, immutable: Boolean)

final case class TypeEnvironment private (
  scopes: List[Map[String, TypeBinding]]
):
  def lookup(name: String): Option[TypeBinding] =
    scopes.collectFirst(Function.unlift(_.get(name)))

  def declare(name: String, binding: TypeBinding): TypeEnvironment =
    scopes match
      case currentScope :: outerScopes =>
        TypeEnvironment((currentScope + (name -> binding)) :: outerScopes)
      case Nil => TypeEnvironment(List(Map(name -> binding)))

  def update(name: String, binding: TypeBinding): TypeEnvironment =
    def updateFirst(scopeList: List[Map[String, TypeBinding]]): List[Map[String, TypeBinding]] =
      scopeList match
        case currentScope :: outerScopes if currentScope.contains(name) =>
          (currentScope + (name -> binding)) :: outerScopes
        case currentScope :: outerScopes => currentScope :: updateFirst(outerScopes)
        case Nil => Nil

    TypeEnvironment(updateFirst(scopes))

  def pushScope: TypeEnvironment = TypeEnvironment(Map.empty[String, TypeBinding] :: scopes)

  def popScope: TypeEnvironment =
    scopes match
      case _ :: outerScopes if outerScopes.nonEmpty => TypeEnvironment(outerScopes)
      case _ => this

object TypeEnvironment:
  val empty: TypeEnvironment = TypeEnvironment(List(Map.empty))
