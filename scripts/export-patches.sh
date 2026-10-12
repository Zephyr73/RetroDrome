#!/usr/bin/env bash
# RetroDrome - Export Feature Commits to patches/
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PATCH_DIR="${ROOT_DIR}/patches"

cd "${ROOT_DIR}"

BASE_REF="${1:-upstream/dev}"

echo "==> Exporting commits on top of ${BASE_REF} to ${PATCH_DIR}..."
rm -f "${PATCH_DIR}"/*.patch
git format-patch -o "${PATCH_DIR}" "${BASE_REF}..HEAD"

echo "==> Patches exported successfully:"
ls -l "${PATCH_DIR}"/*.patch
