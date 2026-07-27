#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
version=${1:-0.1.0}
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

"$root/scripts/package-release.sh" "$version" "$work/release"
"$root/scripts/build-pages.sh" "$version" "$work/release" "$work/pages"

jvm_archive="spnuts-$version-jvm.tar.gz"
native_archive="spnuts-$version-linux-x86_64.tar.gz"
test -f "$work/pages/index.html"
test -f "$work/pages/style.css"
test -f "$work/pages/LICENSE"
test -f "$work/pages/downloads/$jvm_archive"
test -f "$work/pages/downloads/$native_archive"
test -f "$work/pages/downloads/SHA256SUMS"
test -f "$work/pages/downloads/manifest.json"

grep -F "SPnuts $version" "$work/pages/index.html"
grep -F "$jvm_archive" "$work/pages/index.html"
grep -F "$native_archive" "$work/pages/index.html"
grep -F "What's new in SPnuts $version" "$work/pages/index.html"
grep -F "Mandatory gradual typing" "$work/pages/index.html"
grep -F 'count = "two"' "$work/pages/index.html"
grep -F "rejected before the chunk runs" "$work/pages/index.html"
cmp "$work/release/$jvm_archive" "$work/pages/downloads/$jvm_archive"
cmp "$work/release/$native_archive" "$work/pages/downloads/$native_archive"
cmp "$work/release/SHA256SUMS" "$work/pages/downloads/SHA256SUMS"
cmp "$root/LICENSE" "$work/pages/LICENSE"
grep -F "\"version\": \"$version\"" "$work/pages/downloads/manifest.json"
