package spnuts.typing

import spnuts.ast.*
import spnuts.typing.StaticType.*

object TypeChecker:
  def check(expr: Expr, environment: TypeEnvironment): TypingResult =
    val checker = new Checker(environment)
    val result = checker.infer(expr, None)
    TypingResult(checker.table, checker.topLevelEnvironment, result)

  private final class Checker(initialEnvironment: TypeEnvironment):
    private final case class FunctionContext(
      expectedReturn: Option[StaticType],
      returns: collection.mutable.ListBuffer[(StaticType, SourcePos)]
    )

    val table: TypeTable = TypeTable()
    private var environment = initialEnvironment
    private var functionContexts = List.empty[FunctionContext]

    def topLevelEnvironment: TypeEnvironment = environment

    def infer(expr: Expr, expected: Option[StaticType]): StaticType =
      val inferred = expr match
        case IntLit(_, _, _) => typed(expr, LongType)
        case FloatLit(_, _, _) => typed(expr, DoubleType)
        case CharLit(_, _) => typed(expr, CharType)
        case StringLit(_, _) => typed(expr, StringType)
        case InterpolatedString(parts, _) =>
          parts.foreach {
            case Right(part) => infer(part, None)
            case Left(_) => ()
          }
          typed(expr, StringType)
        case BoolLit(_, _) => typed(expr, BooleanType)
        case NullLit(_) => typed(expr, NullType)

        case Ident(name, _) =>
          typed(expr, environment.lookup(name).map(_.tpe).getOrElse(AnyType))
        case GlobalRef(name, _) =>
          typed(expr, environment.lookupGlobal(name).map(_.tpe).getOrElse(AnyType))

        case Block(exprs, _) => typed(expr, withScope(inferSequence(exprs)))
        case ExprList(exprs, _) => typed(expr, inferSequence(exprs))

        case VarDecl(kind, name, typeName, value, pos) =>
          val declaredType = typeName.map(normalizeType(_, Set.empty, pos))
          val valueType = infer(value, declaredType)
          val bindingType = declaredType.getOrElse(valueType)
          environment = environment.declare(
            name,
            TypeBinding(bindingType, kind == DeclKind.Val)
          )
          typed(expr, bindingType)

        case Assignment(op, lhs, rhs, pos) =>
          val rhsType = infer(rhs, None)
          val existing = lhs match
            case Ident(name, _) => environment.lookupForAssignment(name)
            case GlobalRef(name, _) => environment.lookupGlobal(name)
            case _ => None

          existing.foreach { binding =>
            if binding.immutable then
              throw TypeError(s"Cannot assign to immutable binding", pos)
          }

          val assignedType =
            if op == AssignOp.Assign then rhsType
            else
              val lhsType = existing.map(_.tpe).getOrElse(infer(lhs, None))
              compoundResult(op, lhsType, rhsType, pos)

          existing.foreach { binding =>
            requireCompatible(
              binding.tpe,
              assignedType,
              pos,
              "Assigned value has an incompatible type"
            )
          }

          lhs match
            case ident @ Ident(name, _) =>
              val bindingType = existing.map(_.tpe).getOrElse(assignedType)
              if existing.isEmpty then
                environment = environment.declare(name, TypeBinding(bindingType, false))
              typed(ident, bindingType)
            case global @ GlobalRef(name, _) =>
              val bindingType = existing.map(_.tpe).getOrElse(assignedType)
              if existing.isEmpty then
                environment =
                  environment.declareGlobal(
                    name,
                    TypeBinding(bindingType, false)
                  )
              typed(global, bindingType)
            case other =>
              if op == AssignOp.Assign then infer(other, None)

          typed(expr, assignedType)

        case MultiAssign(targets, rhs, pos) =>
          val rhsType = infer(rhs, None)
          val targetTypes = rhsType match
            case AnyType | ListType(_) | ArrayType(_) =>
              List.fill(targets.size)(AnyType)
            case scalarType =>
              scalarType :: List.fill(targets.size - 1)(NullType)
          targets.zip(targetTypes).foreach { (target, targetType) =>
            environment.lookupForAssignment(target.name) match
              case Some(binding) if binding.immutable =>
                throw TypeError(s"Cannot assign to immutable binding", pos)
              case Some(binding) =>
                requireCompatible(
                  binding.tpe,
                  targetType,
                  pos,
                  "Assigned value has an incompatible type"
                )
                typed(target, binding.tpe)
              case None =>
                environment = environment.declare(target.name, TypeBinding(targetType, false))
                typed(target, targetType)
          }
          typed(expr, rhsType)

        case BinaryExpr(op, lhs, rhs, pos) =>
          val leftType = infer(lhs, None)
          val leftPackage = environment.activePackage
          val rightType = infer(rhs, None)
          if op == BinOp.LogAnd || op == BinOp.LogOr then
            mergeActivePackages(List(leftPackage, environment.activePackage))
          typed(expr, binaryResult(op, leftType, rightType, pos))

        case UnaryExpr(op, operand, pos) =>
          val operandType = infer(operand, None)
          val resultType = unaryResult(op, operandType, pos)
          op match
            case UnaryOp.PreIncr | UnaryOp.PreDecr | UnaryOp.PostIncr | UnaryOp.PostDecr =>
              operand match
                case Ident(name, _) =>
                  environment.lookupForAssignment(name) match
                    case Some(binding) =>
                      if binding.immutable then
                        throw TypeError(s"Cannot assign to immutable binding", pos)
                      requireCompatible(
                        binding.tpe,
                        resultType,
                        pos,
                        "Increment result has an incompatible type"
                      )
                    case None =>
                      environment =
                        environment.declare(
                          name,
                          TypeBinding(resultType, false)
                        )
                case GlobalRef(name, _) =>
                  environment.lookupGlobal(name) match
                    case Some(binding) =>
                      if binding.immutable then
                        throw TypeError(s"Cannot assign to immutable binding", pos)
                      requireCompatible(
                        binding.tpe,
                        resultType,
                        pos,
                        "Increment result has an incompatible type"
                      )
                    case None =>
                      environment =
                        environment.declareGlobal(
                          name,
                          TypeBinding(resultType, false)
                        )
                case _ => ()
            case _ => ()
          typed(expr, resultType)

        case InstanceofExpr(value, typeName, pos) =>
          validateFixedTypeName(typeName, pos)
          infer(value, None)
          typed(expr, BooleanType)

        case MemberAccess(obj, _, _) =>
          infer(obj, None)
          typed(expr, AnyType)

        case StaticMemberAccess(obj, _, _) =>
          infer(obj, None)
          typed(expr, AnyType)

        case MethodCall(obj, _, args, _) =>
          infer(obj, None)
          args.foreach(infer(_, None))
          typed(expr, AnyType)

        case StaticMethodCall(obj, _, args, _) =>
          infer(obj, None)
          args.foreach(infer(_, None))
          typed(expr, AnyType)

        case ListExpr(elements, _, _) =>
          val elementTypes = elements.map(infer(_, None))
          typed(expr, ListType(TypeRules.joinAll(elementTypes, AnyType)))

        case MapExpr(entries, _) =>
          val inferredEntries = entries.map { (key, value) =>
            infer(key, None) -> infer(value, None)
          }
          val (keyTypes, valueTypes) = inferredEntries.unzip
          typed(
            expr,
            MapType(
              TypeRules.joinAll(keyTypes, AnyType),
              TypeRules.joinAll(valueTypes, AnyType)
            )
          )

        case IndexAccess(obj, index, pos) =>
          val objectType = infer(obj, None)
          val indexType = infer(index, None)
          val resultType = objectType match
            case ListType(elementType) =>
              requireNumeric(indexType, pos, "List index must be numeric")
              elementType
            case ArrayType(elementType) =>
              requireNumeric(indexType, pos, "Array index must be numeric")
              elementType
            case MapType(keyType, valueType) =>
              requireCompatible(keyType, indexType, pos, "Map index has an incompatible type")
              valueType
            case StringType =>
              requireNumeric(indexType, pos, "String index must be numeric")
              CharType
            case AnyType => AnyType
            case other =>
              dynamicAccessResult(other, obj.pos, "Cannot index this receiver type")
          typed(expr, resultType)

        case RangeAccess(obj, from, to, pos) =>
          val objectType = infer(obj, None)
          requireNumeric(infer(from, None), pos, "Range start must be numeric")
          to.foreach(bound =>
            requireNumeric(infer(bound, None), pos, "Range end must be numeric")
          )
          val resultType = objectType match
            case listType @ ListType(_) => listType
            case arrayType @ ArrayType(_) => arrayType
            case StringType => StringType
            case other =>
              dynamicAccessResult(other, obj.pos, "Cannot slice this receiver type")
          typed(expr, resultType)

        case RangeExpr(from, to, pos) =>
          requireNumeric(infer(from, None), pos, "Range start must be numeric")
          requireNumeric(infer(to, None), pos, "Range end must be numeric")
          typed(expr, ArrayType(LongType))

        case TernaryExpr(cond, thenExpr, elseExpr, _) =>
          requireBoolean(infer(cond, None), cond.pos, "Ternary condition must be boolean")
          val branchPackage = environment.activePackage
          val (thenType, thenPackage) =
            inferFromPackage(branchPackage)(infer(thenExpr, None))
          val (elseType, elsePackage) =
            inferFromPackage(branchPackage)(infer(elseExpr, None))
          mergeActivePackages(List(thenPackage, elsePackage))
          typed(expr, TypeRules.join(thenType, elseType))

        case IfExpr(cond, thenBranch, elseIfs, elseBranch, _) =>
          requireBoolean(infer(cond, None), cond.pos, "If condition must be boolean")
          val branchEntry = environment.activePackage
          val (thenType, thenPackage) =
            inferFromPackage(branchEntry)(infer(thenBranch, None))
          var falsePathPackage = branchEntry
          val elseIfResults = elseIfs.map { (elseIfCond, branch) =>
            environment = environment.withActivePackage(falsePathPackage)
            requireBoolean(
              infer(elseIfCond, None),
              elseIfCond.pos,
              "Else-if condition must be boolean"
            )
            falsePathPackage = environment.activePackage
            inferFromPackage(falsePathPackage)(infer(branch, None))
          }
          val (elseType, elsePackage) =
            elseBranch match
              case Some(branch) =>
                inferFromPackage(falsePathPackage)(infer(branch, None))
              case None => NullType -> falsePathPackage
          val branchTypes = thenType :: elseIfResults.map(_._1) ::: List(elseType)
          mergeActivePackages(
            thenPackage :: elseIfResults.map(_._2) ::: List(elsePackage)
          )
          typed(expr, TypeRules.joinAll(branchTypes, NullType))

        case SwitchExpr(target, cases, _) =>
          infer(target, None)
          val branchEntry = environment.activePackage
          val branchResults = cases.map { switchCase =>
            inferFromPackage(branchEntry) {
              switchCase.labels.foreach(_.foreach(infer(_, None)))
              infer(switchCase.body, None)
            }
          }
          val branchTypes = branchResults.map(_._1)
          val branchPackages = branchResults.map(_._2)
          val possibleTypes =
            if cases.exists(_.labels.contains(None)) then branchTypes
            else branchTypes :+ NullType
          val possiblePackages =
            if cases.exists(_.labels.contains(None)) then branchPackages
            else branchPackages :+ branchEntry
          mergeActivePackages(possiblePackages)
          typed(expr, TypeRules.joinAll(possibleTypes, NullType))

        case WhileExpr(cond, body, _) =>
          requireBoolean(infer(cond, None), cond.pos, "While condition must be boolean")
          val loopEntry = environment.activePackage
          val (bodyType, bodyPackage) =
            inferFromPackage(loopEntry)(infer(body, None))
          mergeActivePackages(List(loopEntry, bodyPackage))
          typed(expr, TypeRules.join(bodyType, NullType))

        case DoWhileExpr(body, cond, _) =>
          val loopEntry = environment.activePackage
          val (bodyType, bodyPackage) =
            inferFromPackage(loopEntry)(infer(body, None))
          requireBoolean(infer(cond, None), cond.pos, "Do-while condition must be boolean")
          mergeActivePackages(
            List(loopEntry, bodyPackage, environment.activePackage)
          )
          typed(expr, TypeRules.join(bodyType, NullType))

        case ForExpr(init, cond, update, body, _) =>
          val resultType = withScope {
            init.foreach(infer(_, None))
            cond.foreach(condition =>
              requireBoolean(
                infer(condition, None),
                condition.pos,
                "For condition must be boolean"
              )
            )
            val loopEntry = environment.activePackage
            val (bodyType, bodyPackage) =
              inferFromPackage(loopEntry) {
                val inferredBody = infer(body, None)
                update.foreach(infer(_, None))
                inferredBody
              }
            mergeActivePackages(List(loopEntry, bodyPackage))
            TypeRules.join(bodyType, NullType)
          }
          typed(expr, resultType)

        case ForEachExpr(vars, iterable, body, _) =>
          val iterableType = infer(iterable, None)
          val loopEntry = environment.activePackage
          val resultType = withScope {
            declareForEachTargets(vars, iterableType)
            val (bodyType, bodyPackage) =
              inferFromPackage(loopEntry)(infer(body, None))
            mergeActivePackages(List(loopEntry, bodyPackage))
            TypeRules.join(bodyType, NullType)
          }
          typed(expr, resultType)

        case ForeachExpr(varName, iterable, body, _) =>
          val iterableType = infer(iterable, None)
          val loopEntry = environment.activePackage
          val resultType = withScope {
            declareForEachTargets(List(varName), iterableType)
            val (bodyType, bodyPackage) =
              inferFromPackage(loopEntry)(infer(body, None))
            mergeActivePackages(List(loopEntry, bodyPackage))
            TypeRules.join(bodyType, NullType)
          }
          typed(expr, resultType)

        case FuncDef(name, params, varargs, body, pos, typeParams, paramTypes, returnType) =>
          val typeVariables = typeParams.toSet
          val normalizedParams = params.indices.map { index =>
            paramTypes
              .lift(index)
              .flatten
              .map(normalizeType(_, typeVariables, pos))
              .getOrElse(AnyType)
          }.toList
          val normalizedReturn =
            returnType.map(normalizeType(_, typeVariables, pos))
          val (fixedParams, varargElement) =
            if varargs && normalizedParams.nonEmpty then
              normalizedParams.dropRight(1) -> normalizedParams.lastOption
            else
              normalizedParams -> None
          val provisionalType =
            FunctionType(
              fixedParams,
              normalizedReturn.getOrElse(AnyType),
              varargElement
            )
          val keepDynamicBinding = name
            .flatMap(environment.lookup)
            .exists { existing =>
              existing.tpe match
                case AnyType => true
                case existingFunction: FunctionType =>
                  !usesSameRuntimeSlot(existingFunction, provisionalType)
                case _ => false
            }

          name.foreach { functionName =>
            environment = environment.declare(
              functionName,
              TypeBinding(
                if keepDynamicBinding then AnyType else provisionalType,
                false
              )
            )
          }

          val context = FunctionContext(
            normalizedReturn,
            collection.mutable.ListBuffer.empty
          )
          val bodyType = withRestoredPackage {
            withFunctionContext(context) {
              withScope {
                params.zip(normalizedParams).zipWithIndex.foreach {
                  case ((paramName, paramType), index) =>
                    val bindingType =
                      if varargs && index == params.length - 1 then ArrayType(paramType)
                      else paramType
                    environment = environment.declare(
                      paramName,
                      TypeBinding(bindingType, false)
                    )
                }
                infer(body, None)
              }
            }
          }
          val inferredReturns = context.returns.toList :+ (bodyType -> body.pos)
          context.expectedReturn.filterNot(_ == UnitType).foreach { expectedReturn =>
            inferredReturns.foreach { (actualReturn, returnPos) =>
              requireCompatible(
                expectedReturn,
                actualReturn,
                returnPos,
                "Function return has an incompatible type"
              )
            }
          }
          val finalType =
            FunctionType(
              fixedParams,
              normalizedReturn.getOrElse(
                TypeRules.joinAll(inferredReturns.map(_._1), AnyType)
              ),
              varargElement
            )
          name.foreach { functionName =>
            environment = environment.update(
              functionName,
              TypeBinding(
                if keepDynamicBinding then AnyType else finalType,
                false
              )
            )
          }
          typed(expr, finalType)

        case FuncCall(func, args, pos) =>
          val functionType = infer(func, None)
          val argumentTypes = args.map(infer(_, None))
          val resultType = functionType match
            case FunctionType(parameters, result, None) =>
              requireArity(parameters.length, argumentTypes.length, pos)
              val bindings = collection.mutable.Map.empty[String, StaticType]
              bindTypeVariables(parameters, args, argumentTypes, bindings)
              val resolvedParameters =
                parameters.map(substituteTypeVariables(_, bindings.toMap))
              requireArguments(resolvedParameters, args, argumentTypes)
              substituteTypeVariables(result, bindings.toMap)
            case FunctionType(parameters, result, Some(varargElement)) =>
              if argumentTypes.length < parameters.length then
                throw TypeError(
                  s"Function expects at least ${parameters.length} arguments but got ${argumentTypes.length}",
                  pos
                )
              val bindings = collection.mutable.Map.empty[String, StaticType]
              bindTypeVariables(
                parameters,
                args.take(parameters.length),
                argumentTypes.take(parameters.length),
                bindings
              )
              args.drop(parameters.length)
                .zip(argumentTypes.drop(parameters.length))
                .foreach { (argument, argumentType) =>
                  bindTypeVariables(
                    varargElement,
                    argumentType,
                    argument.pos,
                    bindings
                  )
                }
              val resolvedParameters =
                parameters.map(substituteTypeVariables(_, bindings.toMap))
              val resolvedVararg =
                substituteTypeVariables(varargElement, bindings.toMap)
              requireArguments(
                resolvedParameters,
                args.take(parameters.length),
                argumentTypes.take(parameters.length)
              )
              args.drop(parameters.length)
                .zip(argumentTypes.drop(parameters.length))
                .foreach { (argument, argumentType) =>
                  requireCompatible(
                    resolvedVararg,
                    argumentType,
                    argument.pos,
                    "Function argument has an incompatible type"
                  )
                }
              substituteTypeVariables(result, bindings.toMap)
            case AnyType => AnyType
            case other =>
              throw TypeError(
                "Cannot call a non-function value",
                func.pos,
                actual = Some(other)
              )
          typed(expr, resultType)

        case NewExpr(className, dims, args, classBody, pos) =>
          val resultType = fixedNamedType(className, pos)
          dims.foreach(infer(_, None))
          args.foreach(infer(_, None))
          classBody.foreach(inferClassBody(_, pos))
          typed(expr, resultType)

        case CastExpr(typeName, _, value, pos) =>
          val resultType = fixedNamedType(typeName, pos)
          infer(value, None)
          typed(expr, resultType)

        case ClassExpr(value, _) =>
          infer(value, None)
          typed(expr, AnyType)

        case ClassRef(name, pos) =>
          typed(expr, fixedNamedType(name, pos))

        case BeanDef(typeName, props, pos) =>
          val resultType = fixedNamedType(typeName, pos)
          props.foreach(property => infer(property.value, None))
          typed(expr, resultType)

        case ClassDef(_, superClass, interfaces, body, pos) =>
          superClass.foreach(validateFixedTypeName(_, pos))
          interfaces.foreach(validateFixedTypeName(_, pos))
          inferClassBody(body, pos)
          typed(expr, UnitType)

        case RecordDef(name, fields, pos) =>
          fields.flatMap(_.typeName).foreach { typeName =>
            validateFixedTypeName(List(typeName), pos)
          }
          environment = environment.declare(
            name,
            TypeBinding(AnyType, false)
          )
          typed(expr, UnitType)

        case PackageExpr(parts, dynamic, _) =>
          dynamic match
            case Some(packageName) =>
              infer(packageName, None)
              environment = environment.inDynamicPackage
            case None =>
              environment = environment.inPackage(parts.mkString("."))
          typed(expr, UnitType)

        case ImportExpr(_, _, _, dynamic, _) =>
          dynamic.foreach(infer(_, None))
          typed(expr, UnitType)

        case TryExpr(body, catches, finallyBlock, _) =>
          val branchEntry = environment.activePackage
          val (bodyType, bodyPackage) =
            inferFromPackage(branchEntry)(infer(body, None))
          val catchResults = catches.map { catchClause =>
            inferFromPackage(branchEntry) {
              withScope {
                val caughtType =
                  catchClause.exType
                    .map(normalizeType(_, Set.empty, catchClause.pos))
                    .getOrElse(AnyType)
                environment = environment.declare(
                  catchClause.varName,
                  TypeBinding(caughtType, false)
                )
                infer(catchClause.body, None)
              }
            }
          }
          val catchTypes = catchResults.map(_._1)
          mergeActivePackages(bodyPackage :: catchResults.map(_._2))
          finallyBlock.foreach(infer(_, None))
          typed(expr, TypeRules.joinAll(bodyType :: catchTypes, bodyType))

        case ThrowExpr(value, _) =>
          typed(expr, value.map(infer(_, None)).getOrElse(UnitType))

        case CatchExpr(cls, handler, _) =>
          infer(cls, None)
          infer(handler, None)
          typed(expr, AnyType)

        case FinallyExpr(body, finalizer, _) =>
          val bodyType = infer(body, None)
          finalizer.foreach(infer(_, None))
          typed(expr, bodyType)

        case ReturnExpr(value, pos) =>
          val returnType = value.map(infer(_, None)).getOrElse(UnitType)
          functionContexts.headOption.foreach(_.returns += (returnType -> pos))
          typed(expr, returnType)

        case YieldExpr(value, _) =>
          typed(expr, value.map(infer(_, None)).getOrElse(UnitType))

        case BreakExpr(value, _) =>
          typed(expr, value.map(infer(_, None)).getOrElse(UnitType))

        case ContinueExpr(_) => typed(expr, UnitType)

      expected.foreach { expectedType =>
        requireCompatible(expectedType, inferred, expr.pos, "Expression has an incompatible type")
      }
      inferred

    private def inferSequence(exprs: List[Expr]): StaticType =
      exprs.foldLeft[StaticType](UnitType)((_, current) => infer(current, None))

    private def withScope[A](body: => A): A =
      environment = environment.pushScope
      try body
      finally environment = environment.popScope

    private def withFunctionContext[A](context: FunctionContext)(body: => A): A =
      functionContexts = context :: functionContexts
      try body
      finally functionContexts = functionContexts.tail

    private def withRestoredPackage[A](body: => A): A =
      val savedPackage = environment.activePackage
      try body
      finally environment = environment.withActivePackage(savedPackage)

    private def inferFromPackage[A](
      packageName: Option[String]
    )(body: => A): (A, Option[String]) =
      environment = environment.withActivePackage(packageName)
      val result = body
      result -> environment.activePackage

    private def mergeActivePackages(
      packages: Iterable[Option[String]]
    ): Unit =
      val distinctPackages = packages.toSet
      val mergedPackage =
        if distinctPackages.size == 1 then distinctPackages.head
        else None
      environment = environment.withActivePackage(mergedPackage)

    private def requireArity(expected: Int, actual: Int, pos: SourcePos): Unit =
      if expected != actual then
        throw TypeError(
          s"Function expects $expected arguments but got $actual",
          pos
        )

    private def requireArguments(
      parameters: List[StaticType],
      arguments: List[Expr],
      argumentTypes: List[StaticType]
    ): Unit =
      parameters.zip(arguments).zip(argumentTypes).foreach {
        case ((parameterType, argument), argumentType) =>
          requireCompatible(
            parameterType,
            argumentType,
            argument.pos,
            "Function argument has an incompatible type"
          )
      }

    private def bindTypeVariables(
      parameters: List[StaticType],
      arguments: List[Expr],
      argumentTypes: List[StaticType],
      bindings: collection.mutable.Map[String, StaticType]
    ): Unit =
      parameters.zip(arguments).zip(argumentTypes).foreach {
        case ((parameterType, argument), argumentType) =>
          bindTypeVariables(parameterType, argumentType, argument.pos, bindings)
      }

    private def bindTypeVariables(
      parameterType: StaticType,
      argumentType: StaticType,
      pos: SourcePos,
      bindings: collection.mutable.Map[String, StaticType]
    ): Unit =
      (parameterType, argumentType) match
        case (TypeVariable(name), actualType) =>
          bindings.get(name) match
            case None => bindings(name) = actualType
            case Some(existingType)
                if TypeRules.isCompatible(existingType, actualType) &&
                  TypeRules.isCompatible(actualType, existingType) =>
              ()
            case Some(existingType) =>
              throw TypeError(
                s"Conflicting binding for type variable $name",
                pos,
                Some(existingType),
                Some(actualType)
              )
        case (ListType(parameterElement), ListType(argumentElement)) =>
          bindTypeVariables(parameterElement, argumentElement, pos, bindings)
        case (
              MapType(parameterKey, parameterValue),
              MapType(argumentKey, argumentValue)
            ) =>
          bindTypeVariables(parameterKey, argumentKey, pos, bindings)
          bindTypeVariables(parameterValue, argumentValue, pos, bindings)
        case (ArrayType(parameterElement), ArrayType(argumentElement)) =>
          bindTypeVariables(parameterElement, argumentElement, pos, bindings)
        case (
              FunctionType(parameterParams, parameterResult, parameterVararg),
              FunctionType(argumentParams, argumentResult, argumentVararg)
            ) if parameterParams.length == argumentParams.length &&
              parameterVararg.isDefined == argumentVararg.isDefined =>
          parameterParams.zip(argumentParams).foreach {
            (nestedParameter, nestedArgument) =>
              bindTypeVariables(nestedParameter, nestedArgument, pos, bindings)
          }
          bindTypeVariables(parameterResult, argumentResult, pos, bindings)
          parameterVararg.zip(argumentVararg).foreach {
            (nestedParameter, nestedArgument) =>
              bindTypeVariables(nestedParameter, nestedArgument, pos, bindings)
          }
        case (
              NamedType(parameterName, parameterArguments),
              NamedType(argumentName, argumentArguments)
            ) if parameterName == argumentName &&
              parameterArguments.length == argumentArguments.length =>
          parameterArguments.zip(argumentArguments).foreach {
            (nestedParameter, nestedArgument) =>
              bindTypeVariables(nestedParameter, nestedArgument, pos, bindings)
          }
        case _ => ()

    private def substituteTypeVariables(
      tpe: StaticType,
      bindings: Map[String, StaticType]
    ): StaticType =
      tpe match
        case TypeVariable(name) => bindings.getOrElse(name, tpe)
        case ListType(element) =>
          ListType(substituteTypeVariables(element, bindings))
        case MapType(key, value) =>
          MapType(
            substituteTypeVariables(key, bindings),
            substituteTypeVariables(value, bindings)
          )
        case ArrayType(element) =>
          ArrayType(substituteTypeVariables(element, bindings))
        case FunctionType(parameters, result, varargElement) =>
          FunctionType(
            parameters.map(substituteTypeVariables(_, bindings)),
            substituteTypeVariables(result, bindings),
            varargElement.map(substituteTypeVariables(_, bindings))
          )
        case NamedType(name, arguments) =>
          NamedType(
            name,
            arguments.map(substituteTypeVariables(_, bindings))
          )
        case _ => tpe

    private def fixedNamedType(name: List[String], pos: SourcePos): StaticType =
      validateFixedTypeName(name, pos)
      if name.nonEmpty then NamedType(name.mkString("."))
      else AnyType

    private val forbiddenPrimitiveNames =
      Set("byte", "char", "short", "int", "float", "double", "long", "boolean", "void")

    private def normalizeType(
      expr: TypeExpr,
      typeVariables: Set[String],
      pos: SourcePos
    ): StaticType =
      validateTypeExpr(expr, pos)
      StaticType.fromTypeExpr(expr, typeVariables)

    private def validateFixedTypeName(name: List[String], pos: SourcePos): Unit =
      validateTypeExpr(TypeExpr(name), pos)

    private def validateTypeExpr(expr: TypeExpr, pos: SourcePos): Unit =
      val name = expr.name.mkString(".")
      if forbiddenPrimitiveNames.contains(name) then
        throw TypeError(
          s"Primitive type '$name' is not allowed; use '${name.head.toUpper}${name.tail}' instead",
          pos
        )
      expr.typeArgs.foreach(validateTypeExpr(_, pos))

    private def usesSameRuntimeSlot(
      existing: FunctionType,
      current: FunctionType
    ): Boolean =
      (existing.varargElement, current.varargElement) match
        case (Some(_), Some(_)) => true
        case (None, None) => existing.parameters.length == current.parameters.length
        case _ => false

    private def inferClassBody(body: ClassDefBody, pos: SourcePos): Unit =
      val classContext =
        FunctionContext(None, collection.mutable.ListBuffer.empty)
      withRestoredPackage {
        withFunctionContext(classContext) {
          withScope {
            body.fields.foreach { field =>
              field.typeName.foreach(validateFixedTypeName(_, pos))
              val fieldType =
                field.init.map(infer(_, None)).getOrElse(
                  field.typeName
                    .map(parts => StaticType.fromTypeExpr(TypeExpr(parts)))
                    .getOrElse(AnyType)
                )
              environment = environment.declare(
                field.name,
                TypeBinding(fieldType, false)
              )
            }
            body.methods.foreach { method =>
              method.returnType.foreach(validateFixedTypeName(_, pos))
              val methodContext =
                FunctionContext(None, collection.mutable.ListBuffer.empty)
              withRestoredPackage {
                withFunctionContext(methodContext) {
                  withScope {
                    method.params.foreach { (typeName, name) =>
                      typeName.foreach(validateFixedTypeName(_, pos))
                      val parameterType =
                        typeName
                          .map(parts => StaticType.fromTypeExpr(TypeExpr(parts)))
                          .getOrElse(AnyType)
                      environment = environment.declare(
                        name,
                        TypeBinding(parameterType, false)
                      )
                    }
                    infer(method.body, None)
                  }
                }
              }
            }
          }
        }
      }

    private def declareForEachTargets(
      names: List[String],
      iterableType: StaticType
    ): Unit =
      val targetTypes = (iterableType, names.size) match
        case (ListType(elementType), 1) => List(elementType)
        case (ArrayType(elementType), 1) => List(elementType)
        case (MapType(keyType, valueType), 2) => List(keyType, valueType)
        case _ => List.fill(names.size)(AnyType)

      names.zip(targetTypes).foreach { (name, targetType) =>
        environment = environment.declare(name, TypeBinding(targetType, false))
      }

    private def dynamicAccessResult(
      receiverType: StaticType,
      pos: SourcePos,
      message: String
    ): StaticType =
      receiverType match
        case UnitType | BooleanType | LongType | DoubleType | CharType | NullType |
            ListType(_) | MapType(_, _) =>
          throw TypeError(message, pos, actual = Some(receiverType))
        case FunctionType(_, _, _) =>
          throw TypeError(message, pos, actual = Some(receiverType))
        case _ => AnyType

    private def compoundResult(
      op: AssignOp,
      left: StaticType,
      right: StaticType,
      pos: SourcePos
    ): StaticType =
      import AssignOp.*
      op match
        case Assign => right
        case AddAssign => binaryResult(BinOp.Add, left, right, pos)
        case SubAssign => binaryResult(BinOp.Sub, left, right, pos)
        case MulAssign => binaryResult(BinOp.Mul, left, right, pos)
        case DivAssign => binaryResult(BinOp.Div, left, right, pos)
        case ModAssign => binaryResult(BinOp.Mod, left, right, pos)
        case AndAssign => binaryResult(BinOp.BitAnd, left, right, pos)
        case OrAssign => binaryResult(BinOp.BitOr, left, right, pos)
        case XorAssign => binaryResult(BinOp.BitXor, left, right, pos)
        case ShiftLeftAssign => binaryResult(BinOp.ShiftLeft, left, right, pos)
        case ShiftRightAssign => binaryResult(BinOp.ShiftRight, left, right, pos)
        case UnsignedShiftRightAssign =>
          binaryResult(BinOp.UnsignedShiftRight, left, right, pos)

    private def binaryResult(
      op: BinOp,
      left: StaticType,
      right: StaticType,
      pos: SourcePos
    ): StaticType =
      import BinOp.*
      op match
        case Add if left == StringType || right == StringType => StringType
        case Add | Sub | Mul | Div | Mod =>
          numericResult(left, right, pos, s"Operator $op requires numeric operands")
        case BitAnd | BitOr | BitXor | ShiftLeft | ShiftRight | UnsignedShiftRight =>
          requireNumeric(left, pos, s"Operator $op requires numeric operands")
          requireNumeric(right, pos, s"Operator $op requires numeric operands")
          LongType
        case Eq | NotEq => BooleanType
        case Lt | Gt | Le | Ge =>
          requireNumeric(left, pos, s"Operator $op requires numeric operands")
          requireNumeric(right, pos, s"Operator $op requires numeric operands")
          BooleanType
        case LogAnd | LogOr =>
          requireBoolean(left, pos, s"Operator $op requires boolean operands")
          requireBoolean(right, pos, s"Operator $op requires boolean operands")
          BooleanType

    private def unaryResult(op: UnaryOp, operand: StaticType, pos: SourcePos): StaticType =
      import UnaryOp.*
      op match
        case Neg =>
          requireNumeric(operand, pos, s"Operator $op requires a numeric operand")
          operand match
            case LongType => LongType
            case DoubleType => DoubleType
            case AnyType => AnyType
            case _ => AnyType
        case BitNot =>
          requireNumeric(operand, pos, s"Operator $op requires a numeric operand")
          LongType
        case LogNot =>
          requireBoolean(operand, pos, s"Operator $op requires a boolean operand")
          BooleanType
        case PreIncr | PreDecr | PostIncr | PostDecr =>
          requireNumeric(operand, pos, s"Operator $op requires a numeric operand")
          operand

    private def numericResult(
      left: StaticType,
      right: StaticType,
      pos: SourcePos,
      message: String
    ): StaticType =
      requireNumeric(left, pos, message)
      requireNumeric(right, pos, message)
      if left == AnyType || right == AnyType then AnyType
      else if left == DoubleType || right == DoubleType then DoubleType
      else LongType

    private def requireNumeric(tpe: StaticType, pos: SourcePos, message: String): Unit =
      tpe match
        case AnyType | LongType | DoubleType => ()
        case other => throw TypeError(message, pos, actual = Some(other))

    private def requireBoolean(tpe: StaticType, pos: SourcePos, message: String): Unit =
      if tpe != AnyType then
        requireCompatible(BooleanType, tpe, pos, message)

    private def requireCompatible(
      expected: StaticType,
      actual: StaticType,
      pos: SourcePos,
      message: String
    ): Unit =
      if !TypeRules.isCompatible(expected, actual) then
        throw TypeError(message, pos, Some(expected), Some(actual))

    private def typed(expr: Expr, tpe: StaticType): StaticType =
      table.record(expr, tpe)
