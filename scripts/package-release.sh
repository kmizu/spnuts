#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "usage: $0 <version> <output-dir>" >&2
  exit 64
fi

version=$1
output_dir=$2
if [[ ! $version =~ ^[0-9]+(\.[0-9]+)+$ ]]; then
  echo "release version must be numeric dotted form, got: $version" >&2
  exit 64
fi

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$root"
build_version=$(sed -nE 's/^ThisBuild \/ version[[:space:]]*:=[[:space:]]*"([^"]+)".*/\1/p' build.sbt)
if [[ $build_version != "$version" ]]; then
  echo "requested version $version does not match build.sbt version $build_version" >&2
  exit 1
fi
mkdir -p "$output_dir"
output_dir=$(cd "$output_dir" && pwd)
stage=$(mktemp -d)
trap 'rm -rf "$stage"' EXIT

jvm_name="spnuts-$version-jvm"
native_name="spnuts-$version-linux-x86_64"
jvm_dir="$stage/$jvm_name"
native_dir="$stage/$native_name"
mkdir -p "$jvm_dir/bin" "$jvm_dir/lib/classes" "$native_dir/bin"
install -m 644 LICENSE "$jvm_dir/LICENSE"
install -m 644 LICENSE "$native_dir/LICENSE"

sbt -batch 'replJVM / Compile / compile' 'replNative / Compile / nativeLink'

classpath=$(sbt -batch 'export replJVM / Runtime / fullClasspath' | awk '/^\// { line = $0 } END { print line }')
if [[ -z $classpath ]]; then
  echo "could not determine replJVM runtime classpath" >&2
  exit 1
fi

IFS=: read -r -a entries <<< "$classpath"
for entry in "${entries[@]}"; do
  if [[ -d $entry ]]; then
    cp -R "$entry"/. "$jvm_dir/lib/classes/"
  elif [[ -f $entry ]]; then
    cp "$entry" "$jvm_dir/lib/"
  else
    echo "runtime classpath entry is missing: $entry" >&2
    exit 1
  fi
done

cat > "$jvm_dir/bin/spnuts" <<'EOF'
#!/usr/bin/env sh
set -eu
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec java -cp "$script_dir/../lib/classes:$script_dir/../lib/*" spnuts.repl.Main "$@"
EOF
chmod 755 "$jvm_dir/bin/spnuts"

native_binary=$(find repl/native/target -type f -path '*/target/scala-*/spnuts-repl' -print -quit)
if [[ -z $native_binary ]]; then
  echo "could not find Scala Native REPL binary" >&2
  exit 1
fi
install -m 755 "$native_binary" "$native_dir/bin/spnuts"

tar -C "$stage" -czf "$output_dir/$jvm_name.tar.gz" "$jvm_name"
tar -C "$stage" -czf "$output_dir/$native_name.tar.gz" "$native_name"
(
  cd "$output_dir"
  sha256sum "$jvm_name.tar.gz" "$native_name.tar.gz" > SHA256SUMS
)
