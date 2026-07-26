#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "usage: $0 <version> <release-dir> <site-output-dir>" >&2
  exit 64
fi

version=$1
release_dir=$2
site_output=$3
if [[ ! $version =~ ^[0-9]+(\.[0-9]+)+$ ]]; then
  echo "release version must be numeric dotted form, got: $version" >&2
  exit 64
fi

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
jvm_archive="spnuts-$version-jvm.tar.gz"
native_archive="spnuts-$version-linux-x86_64.tar.gz"
for required in "$root/site/index.html" "$root/site/style.css" "$root/LICENSE" \
  "$release_dir/$jvm_archive" "$release_dir/$native_archive" "$release_dir/SHA256SUMS"; do
  if [[ ! -f $required ]]; then
    echo "required release-site input is missing: $required" >&2
    exit 1
  fi
done

if [[ -e $site_output ]] && [[ -n $(find "$site_output" -mindepth 1 -maxdepth 1 -print -quit) ]]; then
  echo "site output directory must be empty: $site_output" >&2
  exit 1
fi

mkdir -p "$site_output/downloads"
sed "s/{{VERSION}}/$version/g" "$root/site/index.html" > "$site_output/index.html"
cp "$root/site/style.css" "$site_output/style.css"
cp "$root/LICENSE" "$site_output/LICENSE"
cp "$release_dir/$jvm_archive" "$release_dir/$native_archive" "$release_dir/SHA256SUMS" "$site_output/downloads/"
cat > "$site_output/downloads/manifest.json" <<EOF
{
  "version": "$version",
  "artifacts": [
    "$jvm_archive",
    "$native_archive"
  ],
  "checksums": "SHA256SUMS"
}
EOF
