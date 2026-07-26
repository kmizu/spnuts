package spnuts.typing

final case class TypeBinding(tpe: StaticType, immutable: Boolean)

final case class TypeEnvironment private (
  packageScopes: Map[String, Map[String, TypeBinding]],
  lexicalScopes: List[Map[String, TypeBinding]],
  activePackage: Option[String]
):
  def lookup(name: String): Option[TypeBinding] =
    lexicalScopes
      .collectFirst(Function.unlift(_.get(name)))
      .orElse(lookupInActivePackage(name))

  def lookupGlobal(name: String): Option[TypeBinding] =
    packageScopes.getOrElse("", Map.empty).get(name)

  def declareGlobal(name: String, binding: TypeBinding): TypeEnvironment =
    val globalScope = packageScopes.getOrElse("", Map.empty)
    copy(packageScopes = packageScopes + ("" -> (globalScope + (name -> binding))))

  def lookupForAssignment(name: String): Option[TypeBinding] =
    lexicalScopes
      .collectFirst(Function.unlift(_.get(name)))
      .orElse {
        if lexicalScopes.nonEmpty then lookupInActivePackage(name)
        else
          activePackage.flatMap(packageName =>
            packageScopes.getOrElse(packageName, Map.empty).get(name)
          )
      }

  def declare(name: String, binding: TypeBinding): TypeEnvironment =
    lexicalScopes match
      case currentScope :: outerScopes =>
        copy(lexicalScopes = (currentScope + (name -> binding)) :: outerScopes)
      case Nil =>
        activePackage match
          case Some(packageName) =>
            val currentScope = packageScopes.getOrElse(packageName, Map.empty)
            copy(packageScopes = packageScopes + (
              packageName -> (currentScope + (name -> binding))
            ))
          case None => this

  def update(name: String, binding: TypeBinding): TypeEnvironment =
    updateLexical(name, binding) match
      case Some(updatedScopes) => copy(lexicalScopes = updatedScopes)
      case None =>
        activePackage
          .flatMap(packageName =>
            TypeEnvironment.packageChain(packageName)
              .find(packageScopes.getOrElse(_, Map.empty).contains(name))
          )
          .map { packageName =>
            val updated =
              packageScopes.getOrElse(packageName, Map.empty) + (name -> binding)
            copy(packageScopes = packageScopes + (packageName -> updated))
          }
          .getOrElse(this)

  def pushScope: TypeEnvironment =
    copy(lexicalScopes = Map.empty[String, TypeBinding] :: lexicalScopes)

  def popScope: TypeEnvironment =
    lexicalScopes match
      case _ :: outerScopes => copy(lexicalScopes = outerScopes)
      case Nil => this

  def inPackage(name: String): TypeEnvironment =
    copy(activePackage = Some(TypeEnvironment.normalizePackageName(name)))

  def inDynamicPackage: TypeEnvironment =
    copy(activePackage = None)

  def withActivePackage(packageName: Option[String]): TypeEnvironment =
    copy(activePackage = packageName)

  private def lookupInActivePackage(name: String): Option[TypeBinding] =
    activePackage.flatMap { packageName =>
      TypeEnvironment.packageChain(packageName).collectFirst(
        Function.unlift(candidate =>
          packageScopes.getOrElse(candidate, Map.empty).get(name)
        )
      )
    }

  private def updateLexical(
    name: String,
    binding: TypeBinding
  ): Option[List[Map[String, TypeBinding]]] =
    def updateFirst(
      scopes: List[Map[String, TypeBinding]]
    ): Option[List[Map[String, TypeBinding]]] =
      scopes match
        case currentScope :: outerScopes if currentScope.contains(name) =>
          Some((currentScope + (name -> binding)) :: outerScopes)
        case currentScope :: outerScopes =>
          updateFirst(outerScopes).map(currentScope :: _)
        case Nil => None

    updateFirst(lexicalScopes)

object TypeEnvironment:
  val empty: TypeEnvironment =
    TypeEnvironment(Map("" -> Map.empty), Nil, Some(""))

  private def normalizePackageName(name: String): String =
    name.split('.').iterator.filter(_.nonEmpty).mkString(".")

  private def packageChain(name: String): List[String] =
    val parts =
      normalizePackageName(name).split('.').iterator.filter(_.nonEmpty).toList
    if parts.isEmpty then List("")
    else
      (parts.length to 1 by -1)
        .map(length => parts.take(length).mkString("."))
        .toList :+ ""
