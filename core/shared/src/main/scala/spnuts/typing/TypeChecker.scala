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

        case Block(exprs, _) => typed(expr, inferSequence(exprs))
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

        case _ => typed(expr, AnyType)

      expected.foreach { expectedType =>
        requireCompatible(expectedType, inferred, expr.pos, "Expression has an incompatible type")
      }
      inferred

    private def inferSequence(exprs: List[Expr]): StaticType =
      exprs.foldLeft[StaticType](UnitType)((_, current) => infer(current, None))

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
