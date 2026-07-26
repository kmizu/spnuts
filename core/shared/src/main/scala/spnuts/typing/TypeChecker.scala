package spnuts.typing

import spnuts.ast.*
import spnuts.typing.StaticType.*

object TypeChecker:
  def check(expr: Expr, environment: TypeEnvironment): TypingResult =
    val checker = new Checker(environment)
    val result = checker.infer(expr, None)
    TypingResult(checker.table, checker.topLevelEnvironment, result)

  private final class Checker(initialEnvironment: TypeEnvironment):
    val table: TypeTable = TypeTable()
    private var environment = initialEnvironment

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
          typed(expr, environment.lookup(name).map(_.tpe).getOrElse(AnyType))

        case Block(exprs, _) => typed(expr, withScope(inferSequence(exprs)))
        case ExprList(exprs, _) => typed(expr, inferSequence(exprs))

        case VarDecl(kind, name, typeName, value, _) =>
          val declaredType = typeName.map(StaticType.fromTypeExpr(_))
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
            case Ident(name, _) => environment.lookup(name)
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
              val bindingType = existing.map(_.tpe).getOrElse(rhsType)
              if existing.isEmpty then
                environment = environment.declare(name, TypeBinding(bindingType, false))
              typed(ident, bindingType)
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
            environment.lookup(target.name) match
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
          val rightType = infer(rhs, None)
          typed(expr, binaryResult(op, leftType, rightType, pos))

        case UnaryExpr(op, operand, pos) =>
          val operandType = infer(operand, None)
          val resultType = unaryResult(op, operandType, pos)
          op match
            case UnaryOp.PreIncr | UnaryOp.PreDecr | UnaryOp.PostIncr | UnaryOp.PostDecr =>
              operand match
                case Ident(name, _) =>
                  environment.lookup(name).foreach { binding =>
                    if binding.immutable then
                      throw TypeError(s"Cannot assign to immutable binding", pos)
                    requireCompatible(
                      binding.tpe,
                      resultType,
                      pos,
                      "Increment result has an incompatible type"
                    )
                  }
                case _ => ()
            case _ => ()
          typed(expr, resultType)

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
          val thenType = infer(thenExpr, None)
          val elseType = infer(elseExpr, None)
          typed(expr, TypeRules.join(thenType, elseType))

        case IfExpr(cond, thenBranch, elseIfs, elseBranch, _) =>
          requireBoolean(infer(cond, None), cond.pos, "If condition must be boolean")
          val branchTypes =
            infer(thenBranch, None) ::
              elseIfs.map { (elseIfCond, branch) =>
                requireBoolean(
                  infer(elseIfCond, None),
                  elseIfCond.pos,
                  "Else-if condition must be boolean"
                )
                infer(branch, None)
              } :::
              List(elseBranch.map(infer(_, None)).getOrElse(NullType))
          typed(expr, TypeRules.joinAll(branchTypes, NullType))

        case SwitchExpr(target, cases, _) =>
          infer(target, None)
          val branchTypes = cases.map { switchCase =>
            switchCase.labels.foreach(_.foreach(infer(_, None)))
            infer(switchCase.body, None)
          }
          val possibleTypes =
            if cases.exists(_.labels.contains(None)) then branchTypes
            else branchTypes :+ NullType
          typed(expr, TypeRules.joinAll(possibleTypes, NullType))

        case WhileExpr(cond, body, _) =>
          requireBoolean(infer(cond, None), cond.pos, "While condition must be boolean")
          typed(expr, TypeRules.join(infer(body, None), NullType))

        case DoWhileExpr(body, cond, _) =>
          val bodyType = infer(body, None)
          requireBoolean(infer(cond, None), cond.pos, "Do-while condition must be boolean")
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
            val bodyType = infer(body, None)
            update.foreach(infer(_, None))
            TypeRules.join(bodyType, NullType)
          }
          typed(expr, resultType)

        case ForEachExpr(vars, iterable, body, _) =>
          val iterableType = infer(iterable, None)
          val resultType = withScope {
            declareForEachTargets(vars, iterableType)
            TypeRules.join(infer(body, None), NullType)
          }
          typed(expr, resultType)

        case ForeachExpr(varName, iterable, body, _) =>
          val iterableType = infer(iterable, None)
          val resultType = withScope {
            declareForEachTargets(List(varName), iterableType)
            TypeRules.join(infer(body, None), NullType)
          }
          typed(expr, resultType)

        case TryExpr(body, catches, finallyBlock, _) =>
          val bodyType = infer(body, None)
          val catchTypes = catches.map { catchClause =>
            withScope {
              val caughtType =
                catchClause.exType.map(StaticType.fromTypeExpr(_)).getOrElse(AnyType)
              environment = environment.declare(
                catchClause.varName,
                TypeBinding(caughtType, false)
              )
              infer(catchClause.body, None)
            }
          }
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

        case ReturnExpr(value, _) =>
          typed(expr, value.map(infer(_, None)).getOrElse(UnitType))

        case YieldExpr(value, _) =>
          typed(expr, value.map(infer(_, None)).getOrElse(UnitType))

        case BreakExpr(value, _) =>
          typed(expr, value.map(infer(_, None)).getOrElse(UnitType))

        case ContinueExpr(_) => typed(expr, UnitType)

        case _ => typed(expr, AnyType)

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
