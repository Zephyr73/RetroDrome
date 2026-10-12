#!/usr/bin/env bash
# RetroDrome - Clean Upstream Build with Patches Applied
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PATCH_DIR="${ROOT_DIR}/patches"

cd "${ROOT_DIR}"

BASE_REF="${1:-upstream/dev}"
BUILD_TYPE="${2:-Release}" # Release or Debug

echo "==> Preparing clean build branch from ${BASE_REF}..."
git fetch upstream 2>/dev/null || true

BUILD_BRANCH="retrodrome-build-temp-$(date +%s)"
git checkout -b "${BUILD_BRANCH}" "${BASE_REF}"

cleanup() {
    echo "==> Cleaning up temporary build branch..."
    git reset --hard 2>/dev/null || true
    git clean -fd -e patches -e scripts 2>/dev/null || true
    git checkout - 2>/dev/null || true
    git branch -D "${BUILD_BRANCH}" 2>/dev/null || true
}
trap cleanup EXIT

echo "==> Applying RetroDrome patches..."
for patch in "${PATCH_DIR}"/*.patch; do
    echo "  -> Applying $(basename "${patch}")..."
    git apply "${patch}"
done

echo "==> Building F-Droid ${BUILD_TYPE} APK..."
./gradlew ":app:assembleFdroid${BUILD_TYPE}"

APK_PATH="app/build/outputs/apk/fdroid/${BUILD_TYPE,,}/app-fdroid-${BUILD_TYPE,,}.apk"
if [ -f "${APK_PATH}" ]; then
    echo "==> SUCCESS! Output APK created at: ${APK_PATH}"
else
    echo "==> Build completed. Check app/build/outputs/apk/fdroid/ for artifacts."
fi
