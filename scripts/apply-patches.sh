#!/usr/bin/env bash
# RetroDrome - Apply Patch Layer onto Upstream RetroMusic
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PATCH_DIR="${ROOT_DIR}/patches"

cd "${ROOT_DIR}"

CHECK_ONLY="${1:-}"

PATCHES=("${PATCH_DIR}"/*.patch)
if [ ! -f "${PATCHES[0]}" ]; then
    echo "Error: No patch files found in ${PATCH_DIR}"
    exit 1
fi

echo "==> Validating patches against current tree..."
for patch in "${PATCHES[@]}"; do
    echo "  -> Checking $(basename "${patch}")..."
    git apply --check "${patch}"
done

if [ "${CHECK_ONLY}" = "--check-only" ]; then
    echo "==> All patches validated successfully (check-only mode)."
    exit 0
fi

echo "==> Applying patches..."
for patch in "${PATCHES[@]}"; do
    echo "  -> Applying $(basename "${patch}")..."
    git apply "${patch}"
done

echo "==> All RetroDrome patches applied successfully!"
