#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
version=${1:-0.2.0}

grep -F "ThisBuild / version      := \"$version\"" "$root/build.sbt"
grep -F "SPnuts $version (Scala reimplementation)" "$root/repl/shared/src/main/scala/spnuts/repl/Repl.scala"
grep -F "SPnuts $version (Scala reimplementation)" "$root/README.md"
grep -F "Download SPnuts $version" "$root/README.md"
grep -F "SPnuts $version をダウンロード" "$root/README-ja.md"
grep -F "# SPnuts $version" "$root/docs/releases/$version.md"
grep -F "JDK 17+" "$root/README.md"
grep -F "JDK 17以上" "$root/README-ja.md"
