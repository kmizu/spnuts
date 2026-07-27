#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
workflow="$root/.github/workflows/release.yml"

test -f "$workflow"
grep -F 'tags: ["v*"]' "$workflow"
grep -F 'workflow_dispatch:' "$workflow"
grep -F 'contents: write' "$workflow"
grep -F 'pages: write' "$workflow"
grep -F 'id-token: write' "$workflow"
grep -F 'actions/upload-artifact@v4' "$workflow"
grep -F 'actions/download-artifact@v4' "$workflow"
grep -F 'actions/upload-pages-artifact@v3' "$workflow"
grep -F 'actions/deploy-pages@v4' "$workflow"
grep -F 'gh release create' "$workflow"
grep -F "ref: \${{ github.event_name == 'workflow_dispatch' && format('refs/tags/v{0}', inputs.version) || github.ref }}" "$workflow"
grep -F 'fetch-depth: 0' "$workflow"
grep -F 'git rev-parse --verify --quiet "refs/tags/$tag"' "$workflow"
grep -F 'git rev-list -n 1 "$tag"' "$workflow"
grep -F 'test "$(git rev-parse HEAD)" = "$tag_commit"' "$workflow"
grep -F 'java-version: "17"' "$workflow"
grep -F -- '--verify-tag' "$workflow"
if grep -F -- '--target "$GITHUB_SHA"' "$workflow"; then
  echo "release workflow can auto-create a tag from the triggering SHA" >&2
  exit 1
fi
grep -F 'GH_REPO: ${{ github.repository }}' "$workflow"
grep -F 'scripts/test-release-version.sh' "$workflow"
grep -F 'scripts/test-package-release.sh' "$workflow"
grep -F 'scripts/test-build-pages.sh' "$workflow"
grep -F 'sbt test' "$workflow"
