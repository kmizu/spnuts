# SPnuts

元Sun Microsystemsの戸松豊和さんが開発したスクリプト言語 [Pnuts](https://pnuts.dev.java.net/) を Scala 3 で再実装したプロジェクトです。戸松さんの許可のもと開発しています。

**JVM** と **Scala Native** の両方でビルド可能なクロスプラットフォーム実装です。

## SPnuts 0.1.0 をダウンロード

実行可能な JVM 版・Linux Native 版、チェックサム、導入手順は
[SPnuts 配布ページ](https://kmizu.github.io/spnuts/) で公開しています。
GitHub Release にも同一の配布物を掲載します。

## 特徴

- 手書きPEGパーサー（パーサージェネレーター不使用）
- ツリーウォーキングインタープリター
- クロージャとレキシカルスコープ
- ジェネレーター（`yield`）
- 文字列補間: `"Hello \(name)!"`
- 範囲付きfor-each: `for (x : 1..10)`
- Javaインターロップ（JVM、リフレクション経由）
- 80以上の組み込み関数
- JVM + Scala Native クロスビルド

## クイックスタート

```bash
# REPLを起動
sbt "replJVM/run"

# テストを実行
sbt "coreJVM/test"

# Scala Native向けコンパイル
sbt "coreNative/compile"
```

## 必須の漸進的型付け

SPnuts は、完成した各チャンクを実行する前に必ず型チェックします。型チェックは
常に有効であり、無効化したり有効化を選んだりするフラグはありません。

```pnuts
var count = 1        // Long と推論
count = count + 1    // OK
count = "two"        // このチャンクの実行前に型エラー

val ratio: Double = 1  // Long から Double への拡大変換

function twice(x: Long): Long x * 2

function inspect(value) { // value は Any
  type(value)
}
```

型注釈のない束縛にも固定の型が推論されます。`val` は不変です。`var` と従来形式の
代入（`name = value`）は可変ですが、後から代入する値も束縛の型と互換でなければ
なりません。暗黙の数値拡大変換は `Long` から `Double` への変換だけです。

`Null` は参照型の静的型とは互換ですが、プリミティブ型の静的型とは互換では
ありません。従来形式の代入でも型は推論されて固定されるため、`value = null` と
すると `value` の型は `Null` になり、後から非 null 値を代入すると型エラーになります。

型注釈のない引数や、ホスト・Java・`eval` 連携から来る値など、動的な境界には
`Any` を使います。互換性は構造に沿って再帰的に判定されるため、コレクション型や
関数型の内側でも `Any` を利用できます。`Any` の値が具体的な型注釈の境界を越える
場合は、既存の実行時チェックがその型を保護します。ソース上の空リストの型は
`List<Any>`、AST API から構築した空マップ式の型は `Map<Any, Any>` です。
分岐結果の型に互換性がなければ、結合後の型は `Any` になります。

推論に成功した型は REPL または評価チャンクをまたいで保持され、パッケージごとに
別々に追跡されます。型エラーを含むチャンクは実行されず、そこで推論した型の状態も
公開されません。

## 言語の概要

```pnuts
// 変数
x = 42
name = "世界"

// 文字列補間
println("こんにちは \(name)!")

// 関数定義
function fib(n)
  if (n <= 1) n
  else fib(n - 1) + fib(n - 2)

println(fib(10))  // 55

// クロージャ
double = { x -> x * 2 }
println(double(21))  // 42

// 範囲付きfor-each
sum = 0
for (x : 1..100) sum = sum + x
println(sum)  // 5050

// 高階関数
result = map([1, 2, 3, 4, 5], { n -> n * n })
println(join(result, ", "))  // 1, 4, 9, 16, 25

// ジェネレーター
function range_gen(n) {
  i = 0
  while (i < n) { yield i; i = i + 1 }
}
println(range_gen(5))  // [0, 1, 2, 3, 4]

// マップリテラル
m = { "a" => 1, "b" => 2 }

// try/catch
try {
  throw "エラー"
} catch (java.lang.RuntimeException e) {
  println("捕捉: \(e.getMessage())")
}

// switch文
switch (x) {
  case 1: println("one"); break
  case 2: println("two"); break
  default: println("other")
}
```

## 組み込み関数

| カテゴリ | 関数 |
|---|---|
| 入出力 | `print`, `println`, `p` |
| 型変換 | `str`, `int`, `float`, `boolean`, `char` |
| 型情報 | `type`, `isNull`, `isString`, `isArray`, `isNumber` |
| コレクション生成 | `size`, `length`, `isEmpty`, `array`, `list`, `map` |
| 高階関数 | `map`, `filter`, `reduce`, `each`, `any`, `all` |
| リスト操作 | `sort`, `reverse`, `append`, `get`, `put`, `first`, `last`, `remove`, `copy`, `flatten` |
| マップ操作 | `keys`, `values`, `contains` |
| 範囲 | `range` |
| 文字列 | `join`, `split`, `trim`, `toUpperCase`, `toLowerCase`, `startsWith`, `endsWith`, `indexOf`, `substring`, `replace`, `format`, `concat`, `charAt`, `matches`, `replaceAll` |
| 数学 | `abs`, `max`, `min`, `pow`, `sqrt`, `floor`, `ceil`, `round`, `log`, `sin`, `cos`, `tan`, `PI`, `E`, `random` |
| その他 | `eval`, `generator`, `assert`, `error`, `sleep` |

## プロジェクト構成

```
spnuts/
  build.sbt
  core/
    shared/src/main/scala/spnuts/
      ast/          # シールドトレイトによるAST階層
      parser/       # 手書きレキサー + PEGパーサー
      interpreter/  # ツリーウォーキングインタープリター
      runtime/      # コンテキスト・関数・演算子・組み込み関数
    jvm/            # Javaインターロップ（リフレクション）
    native/         # Scala Nativeスタブ
  repl/
    shared/         # REPLベースクラス
    jvm/            # JLine3 REPL
    native/         # Native REPL
```

## 動作要件

- sbt 1.9以上
- Scala 3.3.1
- JDK 11以上
- （オプション）Scala Native 0.5.6 ツールチェーン（ネイティブビルドの場合）

## 背景

PnutsはSun Microsystems在籍中の戸松豊和さんが開発したJVM向けスクリプト言語です。OracleによるSun買収後、長らくメンテナンスされない状態が続いていました。

以前、[kmizu](https://github.com/kmizu) が戸松さんにコンタクトをとったところ、プロジェクトの引き継ぎを快く承諾していただきました。しかしその後も実装する機会がなかなか訪れませんでした。

そのきっかけとなったのが [Claude Code](https://claude.ai/code) です。Claude Codeをペアプログラミングのパートナーとして活用することで、ようやくScala 3による完全な再実装が実現しました。JavaCC製の文法定義を手書きPEGパーサーに置き換え、Scala Nativeによるクロスプラットフォーム対応も実現しています。

## ライセンス

元のPnutsと同じライセンスに準じます。詳細は [LICENSE](LICENSE) を参照してください。
