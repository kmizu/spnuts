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
grep -F -- '--target "$GITHUB_SHA"' "$workflow"
grep -F 'scripts/test-release-version.sh' "$workflow"
grep -F 'scripts/test-package-release.sh' "$workflow"
grep -F 'scripts/test-build-pages.sh' "$workflow"
