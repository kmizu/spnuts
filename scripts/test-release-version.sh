#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
version=${1:-0.1.0}

grep -F "ThisBuild / version      := \"$version\"" "$root/build.sbt"
grep -F "SPnuts $version (Scala reimplementation)" "$root/repl/shared/src/main/scala/spnuts/repl/Repl.scala"
grep -F "SPnuts $version (Scala reimplementation)" "$root/README.md"
grep -F "JDK 17+" "$root/README.md"
