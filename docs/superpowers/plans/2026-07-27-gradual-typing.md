# Mandatory Gradual Typing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a platform-neutral gradual type checker that runs before every SPnuts program and infers a fixed type even for unannotated bindings.

**Architecture:** A new shared `spnuts.typing` package owns semantic types, immutable session environments, an expression-identity type table, and an AST checker. Public `Interpreter.eval` becomes the mandatory check/execute/commit boundary while private recursive evaluation remains unchecked; REPL, `eval`, `load`, and JVM compilation all enter through the same checker.

**Tech Stack:** Scala 3.3.1, sbt cross-project, ScalaTest 3.2.19, Scala JVM, Scala Native

## Global Constraints

- Type checking is mandatory; do not add a `--check` flag or unchecked normal execution mode.
- Every expression and binding gets a `StaticType`; insufficient information is `Any`.
- Adding annotations constrains inference automatically.
- The only implicit numeric widening is `Long -> Double`.
- Empty collection literals infer `List[Any]` and `Map[Any, Any]`.
- Unannotated function parameters are `Any`; unannotated returns are inferred.
- Java interop, reflection, unknown host bindings, and the caller-visible result of string `eval` are `Any` boundaries.
- Keep the semantic type model in `core/shared`; it must not depend on JVM `Class`.
- Preserve existing runtime checks as defense for values crossing dynamic boundaries.
- Keep the AST unchanged and store inferred expression types in a separate identity-keyed `TypeTable`.
- Run both JVM and Native suites before completion.

---

## File Map

New production files:

- `core/shared/src/main/scala/spnuts/typing/StaticType.scala` — semantic type algebra, annotation normalization, display.
- `core/shared/src/main/scala/spnuts/typing/TypeRules.scala` — compatibility, join, and operator result rules.
- `core/shared/src/main/scala/spnuts/typing/TypeError.scala` — source-positioned static diagnostic.
- `core/shared/src/main/scala/spnuts/typing/TypeEnvironment.scala` — immutable binding/scope state.
- `core/shared/src/main/scala/spnuts/typing/TypingSession.scala` — committed top-level state and check result.
- `core/shared/src/main/scala/spnuts/typing/TypeTable.scala` — expression-identity overlay.
- `core/shared/src/main/scala/spnuts/typing/TypeChecker.scala` — complete conservative AST traversal.

New test files:

- `core/shared/src/test/scala/spnuts/typing/StaticTypeSpec.scala`
- `core/shared/src/test/scala/spnuts/typing/TypeEnvironmentSpec.scala`
- `core/shared/src/test/scala/spnuts/typing/TypeCheckerSpec.scala`

Modified integration files:

- `core/shared/src/main/scala/spnuts/runtime/Context.scala`
- `core/shared/src/main/scala/spnuts/interpreter/Interpreter.scala`
- `core/shared/src/main/scala/spnuts/runtime/BuiltinModule.scala`
- `repl/shared/src/main/scala/spnuts/repl/Repl.scala`
- `core/jvm/src/main/scala/spnuts/compiler/Compiler.scala`
- `core/shared/src/test/scala/spnuts/interpreter/InterpreterSpec.scala`
- `repl/shared/src/test/scala/spnuts/repl/ReplSpec.scala`
- `core/jvm/src/test/scala/spnuts/compiler/CompilerSpec.scala`
- `README.md`
- `README-ja.md`

---

### Task 1: Semantic types, normalization, compatibility, and joins

**Files:**
- Create: `core/shared/src/main/scala/spnuts/typing/StaticType.scala`
- Create: `core/shared/src/main/scala/spnuts/typing/TypeRules.scala`
- Create: `core/shared/src/main/scala/spnuts/typing/TypeError.scala`
- Create: `core/shared/src/test/scala/spnuts/typing/StaticTypeSpec.scala`

**Interfaces:**
- Consumes: `spnuts.ast.TypeExpr`, `spnuts.ast.SourcePos`
- Produces: `StaticType`, `StaticType.fromTypeExpr`, `TypeRules.isCompatible`, `TypeRules.join`, `TypeError`

- [ ] **Step 1: Write the failing semantic-type tests**

Create `StaticTypeSpec.scala` with exact assertions for display names,
annotation normalization, `Any`, nullability, numeric widening, collection
joins, and incompatible joins:

```scala
package spnuts.typing

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spnuts.ast.TypeExpr
import spnuts.typing.StaticType.*

class StaticTypeSpec extends AnyFlatSpec with Matchers:
  "StaticType.fromTypeExpr" should "normalize aliases without JVM reflection" in {
    StaticType.fromTypeExpr(TypeExpr(List("Int"))) shouldBe LongType
    StaticType.fromTypeExpr(TypeExpr(List("Float"))) shouldBe DoubleType
    StaticType.fromTypeExpr(TypeExpr(List("Any"))) shouldBe AnyType
    StaticType.fromTypeExpr(TypeExpr.Wildcard) shouldBe AnyType
    StaticType.fromTypeExpr(TypeExpr.array(TypeExpr(List("String")))) shouldBe
      ArrayType(StringType)
  }

  it should "retain collection, function, named, and type-variable structure" in {
    StaticType.fromTypeExpr(
      TypeExpr(List("List"), List(TypeExpr(List("String"))))
    ) shouldBe ListType(StringType)
    StaticType.fromTypeExpr(
      TypeExpr.func(List(TypeExpr(List("Long"))), TypeExpr(List("Boolean")))
    ) shouldBe FunctionType(List(LongType), BooleanType, None)
    StaticType.fromTypeExpr(
      TypeExpr(List("com", "acme", "Thing"), List(TypeExpr(List("String"))))
    ) shouldBe NamedType("com.acme.Thing", List(StringType))
    StaticType.fromTypeExpr(TypeExpr(List("T")), Set("T")) shouldBe TypeVariable("T")
  }

  "TypeRules.isCompatible" should "allow only the specified gradual and numeric cases" in {
    TypeRules.isCompatible(LongType, LongType) shouldBe true
    TypeRules.isCompatible(DoubleType, LongType) shouldBe true
    TypeRules.isCompatible(LongType, DoubleType) shouldBe false
    TypeRules.isCompatible(StringType, NullType) shouldBe true
    TypeRules.isCompatible(LongType, NullType) shouldBe false
    TypeRules.isCompatible(StringType, AnyType) shouldBe true
    TypeRules.isCompatible(AnyType, StringType) shouldBe true
  }

  "TypeRules.join" should "preserve useful common structure and otherwise use Any" in {
    TypeRules.join(LongType, DoubleType) shouldBe DoubleType
    TypeRules.join(StringType, NullType) shouldBe StringType
    TypeRules.join(ListType(LongType), ListType(DoubleType)) shouldBe
      ListType(DoubleType)
    TypeRules.join(LongType, StringType) shouldBe AnyType
  }
```

- [ ] **Step 2: Run the test and verify it fails because the typing package does not exist**

Run:

```bash
sbt "coreJVM/testOnly spnuts.typing.StaticTypeSpec"
```

Expected: compilation fails with `Not found: StaticType` / missing
`spnuts.typing`.

- [ ] **Step 3: Implement `StaticType`, `TypeRules`, and `TypeError`**

Use this public shape:

```scala
sealed trait StaticType:
  def displayName: String

object StaticType:
  case object AnyType extends StaticType
  case object NullType extends StaticType
  case object UnitType extends StaticType
  case object BooleanType extends StaticType
  case object LongType extends StaticType
  case object DoubleType extends StaticType
  case object CharType extends StaticType
  case object StringType extends StaticType
  final case class ListType(element: StaticType) extends StaticType
  final case class MapType(key: StaticType, value: StaticType) extends StaticType
  final case class ArrayType(element: StaticType) extends StaticType
  final case class FunctionType(
    parameters: List[StaticType],
    result: StaticType,
    varargElement: Option[StaticType] = None
  ) extends StaticType
  final case class NamedType(name: String, arguments: List[StaticType] = Nil)
      extends StaticType
  final case class TypeVariable(name: String) extends StaticType

  def fromTypeExpr(expr: TypeExpr, typeVariables: Set[String] = Set.empty): StaticType
```

`TypeRules` must expose:

```scala
object TypeRules:
  def isReference(tpe: StaticType): Boolean
  def isCompatible(expected: StaticType, actual: StaticType): Boolean
  def join(left: StaticType, right: StaticType): StaticType
  def joinAll(types: Iterable[StaticType], ifEmpty: StaticType): StaticType
```

`TypeError` must expose stable fields used by the REPL:

```scala
final case class TypeError(
  msg: String,
  pos: SourcePos,
  expected: Option[StaticType] = None,
  actual: Option[StaticType] = None
) extends RuntimeException(s"$msg at $pos")
```

- [ ] **Step 4: Run JVM and Native semantic-type tests**

Run:

```bash
sbt "coreJVM/testOnly spnuts.typing.StaticTypeSpec" \
    "coreNative/testOnly spnuts.typing.StaticTypeSpec"
```

Expected: both pass.

- [ ] **Step 5: Commit**

```bash
git add core/shared/src/main/scala/spnuts/typing \
        core/shared/src/test/scala/spnuts/typing/StaticTypeSpec.scala
git commit -m "feat: add platform-neutral static types"
```

---

### Task 2: Immutable typing environments, identity table, and sessions

**Files:**
- Create: `core/shared/src/main/scala/spnuts/typing/TypeEnvironment.scala`
- Create: `core/shared/src/main/scala/spnuts/typing/TypingSession.scala`
- Create: `core/shared/src/main/scala/spnuts/typing/TypeTable.scala`
- Create: `core/shared/src/test/scala/spnuts/typing/TypeEnvironmentSpec.scala`

**Interfaces:**
- Consumes: `StaticType`, `spnuts.ast.Expr`
- Produces: `TypeBinding`, `TypeEnvironment`, `TypingSession`, `TypingResult`, `TypeTable`

- [ ] **Step 1: Write failing environment/session/table tests**

Cover shadowing, immutable bindings, snapshot/commit, and identity rather than
case-class structural equality:

```scala
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
```

- [ ] **Step 2: Run the test and verify missing types fail compilation**

Run:

```bash
sbt "coreJVM/testOnly spnuts.typing.TypeEnvironmentSpec"
```

Expected: missing `TypeEnvironment`, `TypingSession`, and `TypeTable`.

- [ ] **Step 3: Implement the environment and session APIs**

Use immutable scope lists with the head as the current scope:

```scala
final case class TypeBinding(tpe: StaticType, immutable: Boolean)

final case class TypeEnvironment private (
  scopes: List[Map[String, TypeBinding]]
):
  def lookup(name: String): Option[TypeBinding]
  def declare(name: String, binding: TypeBinding): TypeEnvironment
  def update(name: String, binding: TypeBinding): TypeEnvironment
  def pushScope: TypeEnvironment
  def popScope: TypeEnvironment

object TypeEnvironment:
  val empty: TypeEnvironment = TypeEnvironment(List(Map.empty))
```

Use a fresh identity map per check:

```scala
final class TypeTable private ():
  private val entries = new java.util.IdentityHashMap[Expr, StaticType]()
  def record(expr: Expr, tpe: StaticType): StaticType
  def get(expr: Expr): Option[StaticType]
  def apply(expr: Expr): StaticType

object TypeTable:
  def apply(): TypeTable = new TypeTable()
```

The session/check result contracts are:

```scala
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
```

- [ ] **Step 4: Run JVM and Native environment tests**

Run:

```bash
sbt "coreJVM/testOnly spnuts.typing.TypeEnvironmentSpec" \
    "coreNative/testOnly spnuts.typing.TypeEnvironmentSpec"
```

Expected: both pass, proving `IdentityHashMap` is supported on both targets.
If Scala Native lacks that JDK collection, implement a private list of
`(Expr, StaticType)` pairs and compare keys with `eq`; keep the same public API
and rerun both tests.

- [ ] **Step 5: Commit**

```bash
git add core/shared/src/main/scala/spnuts/typing \
        core/shared/src/test/scala/spnuts/typing/TypeEnvironmentSpec.scala
git commit -m "feat: add persistent typing sessions"
```

---

### Task 3: Checker core for literals, declarations, assignments, and operators

**Files:**
- Create: `core/shared/src/main/scala/spnuts/typing/TypeChecker.scala`
- Create: `core/shared/src/test/scala/spnuts/typing/TypeCheckerSpec.scala`

**Interfaces:**
- Consumes: `Expr`, `TypeEnvironment`, `TypeRules`
- Produces: `TypeChecker.check(expr, environment): TypingResult`

- [ ] **Step 1: Write failing checker tests for fixed inferred bindings**

Build ASTs through the real parser and assert both result types and diagnostic
locations:

```scala
package spnuts.typing

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spnuts.parser.Parser
import spnuts.typing.StaticType.*

class TypeCheckerSpec extends AnyFlatSpec with Matchers:
  private def check(code: String, env: TypeEnvironment = TypeEnvironment.empty) =
    TypeChecker.check(Parser.parse(code, "<test>"), env)

  "TypeChecker" should "infer and persist a legacy assignment type" in {
    val result = check("x = 1")
    result.nextEnvironment.lookup("x").map(_.tpe) shouldBe Some(LongType)
  }

  it should "reject an incompatible later assignment before execution" in {
    val error = intercept[TypeError] {
      check("""x = 1
              |x = "later"""".stripMargin)
    }
    error.expected shouldBe Some(LongType)
    error.actual shouldBe Some(StringType)
    error.pos.line shouldBe 2
  }

  it should "infer val and var initializer types and enforce annotations" in {
    check("val x = 1").nextEnvironment.lookup("x") shouldBe
      Some(TypeBinding(LongType, true))
    check("var x = 1").nextEnvironment.lookup("x") shouldBe
      Some(TypeBinding(LongType, false))
    val error = intercept[TypeError](check("""val x: Long = "no""""))
    error.expected shouldBe Some(LongType)
    error.actual shouldBe Some(StringType)
  }

  it should "reject source reassignment of val" in {
    intercept[TypeError](check("val x = 1; x = 2")).msg should include("immutable")
  }

  it should "infer primitive literal and arithmetic result types" in {
    check("1 + 2").resultType shouldBe LongType
    check("1 + 2.0").resultType shouldBe DoubleType
    check("true && false").resultType shouldBe BooleanType
  }

  it should "reject a proven-invalid operator" in {
    val error = intercept[TypeError](check("""true - "x""""))
    error.pos.line shouldBe 1
  }
```

- [ ] **Step 2: Run the checker test and verify it fails**

Run:

```bash
sbt "coreJVM/testOnly spnuts.typing.TypeCheckerSpec"
```

Expected: `TypeChecker` is missing.

- [ ] **Step 3: Implement a stateful private walker behind an immutable API**

Public API:

```scala
object TypeChecker:
  def check(expr: Expr, environment: TypeEnvironment): TypingResult =
    val checker = new Checker(environment)
    val result = checker.infer(expr, None)
    TypingResult(checker.table, checker.topLevelEnvironment, result)
```

The private `Checker` may mutate only its local environment variable. Its
central contract is:

```scala
private def infer(expr: Expr, expected: Option[StaticType]): StaticType
```

Every match arm must finish through:

```scala
private def typed(expr: Expr, tpe: StaticType): StaticType =
  table.record(expr, tpe)
```

Implement literals, identifiers/global references (`Any` if absent), blocks,
expression lists, `VarDecl`, simple identifier `Assignment`, compound
assignments, `MultiAssign`, unary operators, and binary operators.

For assignment:

1. infer the RHS;
2. look up the existing binding;
3. reject immutable or incompatible known types;
4. if missing and the LHS is `Ident`, declare a mutable binding with RHS type;
5. non-identifier lvalues are checked conservatively and return RHS type.

Use a helper that always fills expected/actual on mismatch:

```scala
private def requireCompatible(
  expected: StaticType,
  actual: StaticType,
  pos: SourcePos,
  message: String
): Unit =
  if !TypeRules.isCompatible(expected, actual) then
    throw TypeError(message, pos, Some(expected), Some(actual))
```

Known-invalid numeric/boolean operands throw; an `Any` operand permits the
operation and yields the runtime operation's known result where safe, otherwise
`Any`.

- [ ] **Step 4: Run the checker tests**

Run:

```bash
sbt "coreJVM/testOnly spnuts.typing.TypeCheckerSpec"
```

Expected: all tests added in this task pass.

- [ ] **Step 5: Commit**

```bash
git add core/shared/src/main/scala/spnuts/typing/TypeChecker.scala \
        core/shared/src/test/scala/spnuts/typing/TypeCheckerSpec.scala
git commit -m "feat: infer binding and operator types"
```

---

### Task 4: Collections, indexing, branches, loops, and lexical scopes

**Files:**
- Modify: `core/shared/src/main/scala/spnuts/typing/TypeChecker.scala`
- Modify: `core/shared/src/test/scala/spnuts/typing/TypeCheckerSpec.scala`

**Interfaces:**
- Consumes: Task 3 `infer` and `TypeRules.joinAll`
- Produces: complete core-expression inference with scope restoration

- [ ] **Step 1: Add failing collection/control-flow tests**

Append:

```scala
  it should "infer homogeneous, widened, and empty collections" in {
    check("[1, 2]").resultType shouldBe ListType(LongType)
    check("[1, 2.0]").resultType shouldBe ListType(DoubleType)
    check("[]").resultType shouldBe ListType(AnyType)
    check("""{"x" => 1}""").resultType shouldBe MapType(StringType, LongType)
    TypeChecker.check(
      spnuts.ast.MapExpr(Nil, spnuts.ast.SourcePos("<test>", 1, 1)),
      TypeEnvironment.empty
    ).resultType shouldBe MapType(AnyType, AnyType)
  }

  it should "infer index component types" in {
    check("[1, 2][0]").resultType shouldBe LongType
    check("""{"x" => 1}["x"]""").resultType shouldBe LongType
  }

  it should "join branch types and require Boolean-compatible conditions" in {
    check("if (true) 1 else 2.0").resultType shouldBe DoubleType
    check("""if (true) 1 else "x"""").resultType shouldBe AnyType
    intercept[TypeError](check("if (1) 2 else 3"))
    noException should be thrownBy check("x = eval(\"1\"); if (x) 2 else 3")
  }

  it should "keep block-local loop and catch names out of the top level" in {
    val result = check("for (i : [1, 2]) { val local = i }; 0")
    result.nextEnvironment.lookup("i") shouldBe None
    result.nextEnvironment.lookup("local") shouldBe None
  }
```

- [ ] **Step 2: Run the focused test and observe failures**

Run:

```bash
sbt "coreJVM/testOnly spnuts.typing.TypeCheckerSpec"
```

Expected: collection/control-flow assertions fail or hit unimplemented match
arms.

- [ ] **Step 3: Implement the collection and control-flow match arms**

Implement:

- `ListExpr`, `MapExpr`, `IndexAccess`, `RangeAccess`, `RangeExpr`;
- `TernaryExpr`, `IfExpr`, `SwitchExpr`;
- `WhileExpr`, `DoWhileExpr`, `ForExpr`, `ForEachExpr`, `ForeachExpr`;
- `TryExpr`, catch scopes, `ThrowExpr`, `CatchExpr`, `FinallyExpr`;
- `ReturnExpr`, `YieldExpr`, `BreakExpr`, `ContinueExpr`;
- interpolation as `String`;
- block scope push/pop with `try/finally`.

Use:

```scala
private def withScope[A](body: => A): A =
  environment = environment.pushScope
  try body
  finally environment = environment.popScope
```

When inferring a foreach element:

- `ListType(e)` and `ArrayType(e)` bind the loop variable as `e`;
- `MapType(k, v)` with two target variables binds key/value;
- `Any` or unknown arity binds targets as `Any`.

Conditions accept only `BooleanType` or `AnyType`. Missing `else` contributes
`NullType` to the branch join.

- [ ] **Step 4: Run JVM and Native checker tests**

Run:

```bash
sbt "coreJVM/testOnly spnuts.typing.TypeCheckerSpec" \
    "coreNative/testOnly spnuts.typing.TypeCheckerSpec"
```

Expected: both pass.

- [ ] **Step 5: Commit**

```bash
git add core/shared/src/main/scala/spnuts/typing/TypeChecker.scala \
        core/shared/src/test/scala/spnuts/typing/TypeCheckerSpec.scala
git commit -m "feat: infer collections and control flow"
```

---

### Task 5: Functions, recursion, annotations, and conservative dynamic nodes

**Files:**
- Modify: `core/shared/src/main/scala/spnuts/typing/TypeChecker.scala`
- Modify: `core/shared/src/test/scala/spnuts/typing/TypeCheckerSpec.scala`

**Interfaces:**
- Consumes: `StaticType.FunctionType`, `TypeVariable`, scoped environment
- Produces: exhaustive AST traversal and function checking

- [ ] **Step 1: Add failing function and dynamic-boundary tests**

Append:

```scala
  it should "use Any for unannotated parameters and infer returns" in {
    val result = check("function id(x) x")
    result.nextEnvironment.lookup("id").map(_.tpe) shouldBe
      Some(FunctionType(List(AnyType), AnyType, None))
  }

  it should "check annotated parameters and returns before execution" in {
    check("function add(x: Long, y: Long): Long x + y").resultType shouldBe
      FunctionType(List(LongType, LongType), LongType, None)
    val error = intercept[TypeError] {
      check("""function bad(x: Long): String x + 1""")
    }
    error.expected shouldBe Some(StringType)
    error.actual shouldBe Some(LongType)
  }

  it should "predeclare named functions for recursion" in {
    noException should be thrownBy check(
      "function fact(n: Long): Long if (n <= 1) 1 else n * fact(n - 1)"
    )
  }

  it should "validate known function arity and arguments" in {
    intercept[TypeError] {
      check("""function f(x: Long): Long x; f("bad")""")
    }
    intercept[TypeError] {
      check("function f(x: Long): Long x; f()")
    }
  }

  it should "treat Java, host, and eval results as Any" in {
    check("java.lang.System.currentTimeMillis()").resultType shouldBe AnyType
    check("""eval("1")""").resultType shouldBe AnyType
    check("unknownHostBinding").resultType shouldBe AnyType
  }
```

- [ ] **Step 2: Run the test and observe function failures**

Run:

```bash
sbt "coreJVM/testOnly spnuts.typing.TypeCheckerSpec"
```

Expected: function assertions fail or reach unimplemented arms.

- [ ] **Step 3: Implement function inference and calls**

For `FuncDef`:

1. normalize annotations with the function's `typeParams`;
2. use `Any` for missing parameter annotations;
3. create a provisional result (`declared return` or `Any`);
4. predeclare a named function in the outer environment;
5. push a function scope and declare parameters;
6. collect explicit return types while inferring the body;
7. join explicit returns with the body result when return is unannotated;
8. validate every inferred return against an annotated return;
9. replace the provisional named binding with the final `FunctionType`.

Maintain a private function-context stack:

```scala
private final case class FunctionContext(
  expectedReturn: Option[StaticType],
  returns: collection.mutable.ListBuffer[(StaticType, SourcePos)]
)
```

For `FuncCall`:

- infer function and arguments;
- a known fixed function requires exact arity;
- a known vararg function validates fixed arguments and all remaining
  arguments against `varargElement`;
- `Any`, native/builtin names, and overloaded groups accept the call and return
  `Any`;
- a known non-function value is a type error.

- [ ] **Step 4: Cover every remaining AST constructor conservatively**

Run this inventory and compare it with the `infer` match:

```bash
rg '^case class|^case object' core/shared/src/main/scala/spnuts/ast/Ast.scala
```

Add explicit arms with these results:

- `InstanceofExpr` -> `Boolean`;
- `MemberAccess`, `StaticMemberAccess`, `MethodCall`, and
  `StaticMethodCall` -> infer receiver/arguments, then `Any`;
- `NewExpr`, `CastExpr`, `ClassExpr`, and `ClassRef` -> infer child
  expressions/dimensions/arguments, then `Named` when the class name is
  syntactically fixed and otherwise `Any`;
- `BeanDef` -> infer all property values, then its fixed `Named` type;
- `ClassDef` -> infer every field initializer and method body, then `Unit`;
- `RecordDef` -> declare its factory as `Any`, then `Unit`;
- `PackageExpr` and `ImportExpr` -> infer dynamic name expressions, then
  `Unit`.

Do not add a wildcard `case _ => AnyType`: exhaustive matching is the guard
that future AST nodes receive a deliberate typing rule.

- [ ] **Step 5: Run checker tests and compile with exhaustivity warnings**

Run:

```bash
sbt "coreJVM/testOnly spnuts.typing.TypeCheckerSpec" \
    "coreNative/testOnly spnuts.typing.TypeCheckerSpec" \
    "coreJVM/compile" "coreNative/compile"
```

Expected: all pass and there is no non-exhaustive match warning from
`TypeChecker`.

- [ ] **Step 6: Commit**

```bash
git add core/shared/src/main/scala/spnuts/typing/TypeChecker.scala \
        core/shared/src/test/scala/spnuts/typing/TypeCheckerSpec.scala
git commit -m "feat: type functions and dynamic boundaries"
```

---

### Task 6: Mandatory interpreter gate and atomic typing-session commit

**Files:**
- Modify: `core/shared/src/main/scala/spnuts/runtime/Context.scala`
- Modify: `core/shared/src/main/scala/spnuts/interpreter/Interpreter.scala`
- Modify: `core/shared/src/test/scala/spnuts/interpreter/InterpreterSpec.scala`

**Interfaces:**
- Consumes: `TypingSession`, `TypeChecker.check`
- Produces: mandatory `Interpreter.eval` gate for all interpreter entry points

- [ ] **Step 1: Add failing pre-execution and persistence integration tests**

Add helpers that use a fresh `PnutsPackage` to avoid global leakage, then add:

```scala
  "mandatory gradual typing" should "reject the complete chunk before side effects" in {
    val pkg = PnutsPackage("typing-preflight", Some(PnutsPackage.global))
    val ctx = Context(currentPackage = pkg)
    val error = intercept[TypeError] {
      Interpreter.eval(
        Parser.parse("""sideEffect = 1; x = 1; x = "bad"""", "<test>"),
        ctx
      )
    }
    error.expected shouldBe Some(StaticType.LongType)
    pkg.lookup("sideEffect") shouldBe None
  }

  it should "persist inferred types across successful eval calls" in {
    val pkg = PnutsPackage("typing-session", Some(PnutsPackage.global))
    val ctx = Context(currentPackage = pkg)
    Interpreter.eval(Parser.parse("x = 1", "<first>"), ctx)
    intercept[TypeError] {
      Interpreter.eval(Parser.parse("""x = "bad"""", "<second>"), ctx)
    }
    ctx.getValue("x") shouldBe 1L
  }

  it should "not commit types from a runtime-failed chunk" in {
    val pkg = PnutsPackage("typing-runtime-failure", Some(PnutsPackage.global))
    val ctx = Context(currentPackage = pkg)
    intercept[Throwable] {
      Interpreter.eval(Parser.parse("""x = 1; error("boom")""", "<test>"), ctx)
    }
    noException should be thrownBy {
      Interpreter.eval(Parser.parse("""x = "dynamic after failure"""", "<test>"), ctx)
    }
  }
```

Import `spnuts.typing.{StaticType, TypeError}`.

- [ ] **Step 2: Run the integration tests and verify they fail**

Run:

```bash
sbt "coreJVM/testOnly spnuts.interpreter.InterpreterSpec -- -z 'mandatory gradual typing'"
```

Expected: later assignment throws only after `sideEffect` exists, and typing
does not persist across calls.

- [ ] **Step 3: Add `TypingSession` to `Context`**

Add:

```scala
val typingSession: TypingSession = TypingSession()
```

to `Context`. `cloneForEval()` intentionally constructs a new `Context`, hence
a fresh session.

- [ ] **Step 4: Split public program entry from private recursive evaluation**

Change the public method to:

```scala
def eval(expr: Expr, ctx: Context): Any =
  val checked = TypeChecker.check(expr, ctx.typingSession.snapshot)
  if ctx.callFn == null then
    ctx.callFn = (f, args, c, pos) => callValue(f, args, c, pos)
  val result = evalInner(expr, ctx)
  ctx.typingSession.commit(checked.nextEnvironment)
  result
```

Mechanically change recursive evaluation inside `Interpreter.scala` from
`eval(child, ctx)` to `evalInner(child, ctx)`. Do not change the public calls
in `BuiltinModule.eval`, `JvmPlatform.load`, or `CompiledHelper`: parsed nested
programs must re-enter the mandatory gate.

Verify no recursive public calls remain:

```bash
rg -n '\beval\(' core/shared/src/main/scala/spnuts/interpreter/Interpreter.scala
```

Expected matches: the public definition, external callback/API uses that truly
start a new program if any, and no ordinary AST-child traversal.

- [ ] **Step 5: Run the integration and full core JVM tests**

Run:

```bash
sbt "coreJVM/testOnly spnuts.interpreter.InterpreterSpec" "coreJVM/test"
```

Expected: all pass. Existing dynamic programs must not be rejected merely
because the checker cannot prove a type.

- [ ] **Step 6: Run the core Native tests**

Run:

```bash
sbt "coreNative/test"
```

Expected: all pass.

- [ ] **Step 7: Commit**

```bash
git add core/shared/src/main/scala/spnuts/runtime/Context.scala \
        core/shared/src/main/scala/spnuts/interpreter/Interpreter.scala \
        core/shared/src/test/scala/spnuts/interpreter/InterpreterSpec.scala
git commit -m "feat: require typing before interpretation"
```

---

### Task 7: Nested `eval`/`load` and REPL diagnostics

**Files:**
- Modify: `core/shared/src/test/scala/spnuts/interpreter/InterpreterSpec.scala`
- Modify: `repl/shared/src/main/scala/spnuts/repl/Repl.scala`
- Modify: `repl/shared/src/test/scala/spnuts/repl/ReplSpec.scala`

**Interfaces:**
- Consumes: mandatory public `Interpreter.eval`, `TypeError`
- Produces: no dynamic checker bypass and source-caret type diagnostics

- [ ] **Step 1: Add failing nested-program tests**

In `InterpreterSpec`, add:

```scala
  it should "type-check code inside string eval" in {
    val error = intercept[TypeError] {
      run("""eval("x = 1; x = \"bad\"")""")
    }
    error.expected shouldBe Some(StaticType.LongType)
  }
```

On JVM, add or extend the existing load test to write a temporary file
containing `x = 1; x = "bad"` and assert `load(path)` throws `TypeError` before
the file defines `x`.

- [ ] **Step 2: Add failing REPL type-diagnostic tests**

In `ReplSpec`, add:

```scala
  "gradual typing diagnostics" should "include position, expected/actual, source, and caret" in {
    val repl = new Repl(Context(currentPackage =
      PnutsPackage("repl-typing", Some(PnutsPackage.global))))
    val output = repl.eval("""x = 1
                             |x = "bad"""".stripMargin)
    output should include("Type error at <repl>:2:1")
    output should include("expected Long")
    output should include("String")
    output should include("""x = "bad"""")
    output should include("^")
  }

  it should "retain an inferred type across separate inputs and recover after failure" in {
    val repl = new Repl(Context(currentPackage =
      PnutsPackage("repl-typing-session", Some(PnutsPackage.global))))
    repl.eval("x = 1") shouldBe "1"
    repl.eval("""x = "bad"""") should include("Type error")
    repl.eval("x + 1") shouldBe "2"
  }
```

- [ ] **Step 3: Run focused tests and observe REPL misclassification**

Run:

```bash
sbt "coreJVM/testOnly spnuts.interpreter.InterpreterSpec" \
    "replJVM/testOnly spnuts.repl.ReplSpec"
```

Expected: nested checks pass only after Task 6 refactor; REPL returns generic
`Error:` or fails to render the type diagnostic.

- [ ] **Step 4: Render `TypeError` in the REPL**

Import `spnuts.typing.TypeError` and add before the generic catch:

```scala
case e: TypeError =>
  val detail = (e.expected, e.actual) match
    case (Some(expected), Some(actual)) =>
      s"${e.msg} (expected ${expected.displayName}, actual ${actual.displayName})"
    case _ => e.msg
  formatError("Type error", e.pos, detail, code)
```

Do not catch `TypeError` as `RuntimeError`; keep the diagnostic category
distinct.

- [ ] **Step 5: Run JVM and Native interpreter/REPL tests**

Run:

```bash
sbt "coreJVM/test" "replJVM/test" "coreNative/test" "replNative/test"
```

Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add core/shared/src/test/scala/spnuts/interpreter/InterpreterSpec.scala \
        repl/shared/src/main/scala/spnuts/repl/Repl.scala \
        repl/shared/src/test/scala/spnuts/repl/ReplSpec.scala \
        core/jvm/src/test/scala/spnuts/interpreter
git commit -m "feat: report mandatory type errors in repl"
```

---

### Task 8: Reject proven type errors before JVM bytecode generation

**Files:**
- Modify: `core/jvm/src/main/scala/spnuts/compiler/Compiler.scala`
- Modify: `core/jvm/src/test/scala/spnuts/compiler/CompilerSpec.scala`

**Interfaces:**
- Consumes: `TypeChecker.check`, `TypeEnvironment.empty`, `TypeError`
- Produces: compiler preflight that never turns a type error into fallback

- [ ] **Step 1: Add a failing compiler preflight test**

Add:

```scala
  "Compiler.compileScript" should "throw a proven type error instead of compiling or falling back" in {
    val ast = Parser.parse("""x = 1; x = "bad"""", "<compile-test>")
    val exprs = ast.asInstanceOf[ExprList]
    val error = intercept[spnuts.typing.TypeError] {
      Compiler.compileScript(exprs, PnutsPackage("compiler-typing", None))
    }
    error.expected shouldBe Some(spnuts.typing.StaticType.LongType)
    error.actual shouldBe Some(spnuts.typing.StaticType.StringType)
  }
```

- [ ] **Step 2: Run the test and verify the invalid AST reaches codegen**

Run:

```bash
sbt "coreJVM/testOnly spnuts.compiler.CompilerSpec"
```

Expected: no `TypeError` is thrown.

- [ ] **Step 3: Add compiler preflight outside the fallback catch**

At the start of `compileScript`, before its `try`:

```scala
TypeChecker.check(exprs, TypeEnvironment.empty)
```

Import the typing package. Keep this call outside the broad compilation
`try/catch`, so `TypeError` cannot be logged and converted to `None`.

For `compileFunc`, check the `FuncDef` against an environment whose parameter
types are supplied by the definition before code generation. If doing so
duplicates `TypeChecker` function handling, call:

```scala
TypeChecker.check(func, TypeEnvironment.empty)
```

outside its compilation `try`.

- [ ] **Step 4: Run compiler and full JVM tests**

Run:

```bash
sbt "coreJVM/testOnly spnuts.compiler.CompilerSpec" "coreJVM/test"
```

Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add core/jvm/src/main/scala/spnuts/compiler/Compiler.scala \
        core/jvm/src/test/scala/spnuts/compiler/CompilerSpec.scala
git commit -m "feat: type-check before bytecode generation"
```

---

### Task 9: User documentation and end-to-end verification

**Files:**
- Modify: `README.md`
- Modify: `README-ja.md`

**Interfaces:**
- Consumes: completed language behavior
- Produces: documented mandatory gradual typing contract

- [ ] **Step 1: Add matching English and Japanese gradual-typing sections**

Document these exact runnable examples:

```pnuts
var count = 1        // inferred as Long
count = count + 1    // OK
count = "two"        // Type error before this chunk runs

val ratio: Double = 1  // Long -> Double widening

function twice(x: Long): Long x * 2

function inspect(value) { // value is Any
  type(value)
}
```

State explicitly:

- checking always runs and has no flag;
- unannotated bindings still have inferred fixed types;
- `Any` is used at dynamic boundaries;
- empty collections use `Any` element types;
- incompatible branch types join to `Any`;
- existing runtime checks protect concrete annotations from `Any`;
- `val` is immutable and `var`/legacy assignment is mutable.

Add the equivalent natural Japanese explanation to `README-ja.md`.

- [ ] **Step 2: Run README examples as a smoke test**

Create no permanent fixture. Pipe or place each example in a temporary file
using `mktemp`, then run the built REPL/script entry as already documented by
the repository. Verify the valid examples exit zero and the invalid assignment
prints a source-positioned type error before later output.

- [ ] **Step 3: Run formatting and diff checks**

Run:

```bash
git diff --check
rg -n -- '--check|optional type.?check|型チェック.*任意' README.md README-ja.md
```

Expected: `git diff --check` exits zero; the search finds no wording that
suggests optional checking.

- [ ] **Step 4: Run the complete JVM and Native test suite**

Run:

```bash
sbt test
```

Expected: all JVM and Native tests pass; only the repository's pre-existing
ignored tests remain ignored.

- [ ] **Step 5: Run focused clean compiles**

Run:

```bash
sbt clean coreJVM/compile coreNative/compile replJVM/compile replNative/compile
```

Expected: all compile successfully without new warnings attributable to the
typing implementation.

- [ ] **Step 6: Commit documentation**

```bash
git add README.md README-ja.md
git commit -m "docs: explain mandatory gradual typing"
```

- [ ] **Step 7: Inspect the final branch**

Run:

```bash
git status --short
git log --oneline --decorate origin/main..HEAD
git diff --stat origin/main...HEAD
```

Expected: clean status, focused gradual-typing commits, and only planned files
changed.
