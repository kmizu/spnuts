# SPnuts

元Sun Microsystemsの戸松豊和さんが開発したスクリプト言語 [Pnuts](https://pnuts.dev.java.net/) を Scala 3 で再実装したプロジェクトです。戸松さんの許可のもと開発しています。

**JVM** と **Scala Native** の両方でビルド可能なクロスプラットフォーム実装です。

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

Pnutsは元Sun MicrosystemsのJava組み込みスクリプト言語で、戸松豊和さんが開発しました。本実装はパーサーをJavaCC製から手書きPEGパーサーに置き換え、Scala 3でゼロから再実装したものです。Scala NativeによるJVM非依存バイナリ生成も可能です。

実装者: コウタ（kmizu）— PEG研究の博士号取得者、Onion言語作者、Japan Scala Association理事

## ライセンス

元のPnutsと同じライセンスに準じます。詳細は [LICENSE](LICENSE) を参照してください。
