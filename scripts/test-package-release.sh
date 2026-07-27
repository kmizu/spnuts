#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
version=${1:-0.1.0}
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

if "$root/scripts/package-release.sh" 9.9.9 "$work/mismatched-release"; then
  echo "packager accepted a version that differs from build.sbt" >&2
  exit 1
fi

"$root/scripts/package-release.sh" "$version" "$work/release"

jvm_archive="$work/release/spnuts-$version-jvm.tar.gz"
native_archive="$work/release/spnuts-$version-linux-x86_64.tar.gz"

test -f "$jvm_archive"
test -f "$native_archive"
test -f "$work/release/SHA256SUMS"

(cd "$work/release" && sha256sum -c SHA256SUMS)
tar -tzf "$jvm_archive" | grep -Fx "spnuts-$version-jvm/bin/spnuts"
tar -tzf "$native_archive" | grep -Fx "spnuts-$version-linux-x86_64/bin/spnuts"
tar -tzf "$jvm_archive" | grep -Fx "spnuts-$version-jvm/LICENSE"
tar -tzf "$native_archive" | grep -Fx "spnuts-$version-linux-x86_64/LICENSE"

mkdir "$work/jvm" "$work/native"
tar -xzf "$jvm_archive" -C "$work/jvm"
tar -xzf "$native_archive" -C "$work/native"

cat > "$work/valid.pnuts" <<'EOF'
var count = 1
count = count + 1
println(count)
EOF

cat > "$work/invalid.pnuts" <<'EOF'
println("SHOULD_NOT_RUN_BEFORE")
count = 1
count = "two"
println("SHOULD_NOT_RUN_AFTER")
EOF

verify_launcher() {
  local launcher=$1
  local platform=$2
  local valid_output="$work/$platform-valid.txt"
  local invalid_output="$work/$platform-invalid.txt"

  "$launcher" "$work/valid.pnuts" > "$valid_output"
  grep -Fx '2' "$valid_output"

  "$launcher" "$work/invalid.pnuts" > "$invalid_output"
  grep -F 'Type error at <repl>:3:9:' "$invalid_output"
  grep -F 'expected Long, actual String' "$invalid_output"
  if grep -F 'SHOULD_NOT_RUN_' "$invalid_output"; then
    echo "$platform launcher ran side effects before rejecting the chunk" >&2
    exit 1
  fi
}

verify_launcher "$work/jvm/spnuts-$version-jvm/bin/spnuts" jvm
verify_launcher "$work/native/spnuts-$version-linux-x86_64/bin/spnuts" native
