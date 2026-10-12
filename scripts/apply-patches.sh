#!/usr/bin/env bash
# RetroDrome - Apply Patch Layer onto Upstream RetroMusic
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PATCH_DIR="${ROOT_DIR}/patches"

cd "${ROOT_DIR}"

if [ ! -d "${PATCH_DIR}" ] || [ -z "$(ls -A "${PATCH_DIR}"/*.patch 2>/dev/null)" ]; then
    echo "Error: No patch files found in ${PATCH_DIR}"
    exit 1
fi

# Check if all patches are already applied
ALL_APPLIED=true
for patch in "${PATCH_DIR}"/*.patch; do
    if ! git apply --check --reverse "${patch}" 2>/dev/null; then
        ALL_APPLIED=false
        break
    fi
done

if [ "$ALL_APPLIED" = true ]; then
    echo "==> All RetroDrome patches are already applied to the current tree."
    exit 0
fi

echo "==> Checking patches against current working tree..."
for patch in "${PATCH_DIR}"/*.patch; do
    echo "  -> Validating $(basename "${patch}")..."
    git apply --check "${patch}"
done

echo "==> Applying patches..."
for patch in "${PATCH_DIR}"/*.patch; do
    echo "  -> Applying $(basename "${patch}")..."
    git apply "${patch}"
done

echo "==> All RetroDrome patches applied successfully!"
