#!/usr/bin/env bash
# RetroDrome - Sync & Validate Upstream RetroMusic Bugfixes
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PATCH_DIR="${ROOT_DIR}/patches"

cd "${ROOT_DIR}"

UPSTREAM_BRANCH="${1:-dev}"
DO_REBASE="${2:-}"

echo "==> Checking git remotes..."
if ! git remote | grep -q "^upstream$"; then
    echo "  -> Adding upstream remote (RetroMusicPlayer/RetroMusicPlayer)..."
    git remote add upstream https://github.com/RetroMusicPlayer/RetroMusicPlayer.git
fi

echo "==> Fetching latest changes from upstream..."
git fetch upstream

UPSTREAM_REF="upstream/${UPSTREAM_BRANCH}"
BASE_REF="$(git merge-base HEAD "${UPSTREAM_REF}")"
UPSTREAM_HEAD="$(git rev-parse "${UPSTREAM_REF}")"

if [ "${BASE_REF}" = "${UPSTREAM_HEAD}" ]; then
    echo "==> Up to date! Current branch already includes all commits from ${UPSTREAM_REF}."
    exit 0
fi

NEW_COMMITS="$(git rev-list --count "${BASE_REF}..${UPSTREAM_REF}")"
echo "==> Found ${NEW_COMMITS} new commit(s) / bugfix(es) in ${UPSTREAM_REF}:"
git log --oneline -n 10 "${BASE_REF}..${UPSTREAM_REF}"

echo "==> Testing whether RetroDrome patches apply cleanly on top of newest ${UPSTREAM_REF}..."
TEMP_PATCH_DIR="$(mktemp -d)"
cp -r "${PATCH_DIR}"/*.patch "${TEMP_PATCH_DIR}/"

CURRENT_BRANCH="$(git branch --show-current)"
TEST_BRANCH="retrodrome-sync-test-$(date +%s)"

cleanup() {
    git reset --hard 2>/dev/null || true
    git clean -fd 2>/dev/null || true
    git checkout "${CURRENT_BRANCH}" 2>/dev/null || true
    git branch -D "${TEST_BRANCH}" 2>/dev/null || true
    rm -rf "${TEMP_PATCH_DIR}" 2>/dev/null || true
}
trap cleanup EXIT

git checkout -b "${TEST_BRANCH}" "${UPSTREAM_REF}"

ALL_CLEAN=true
for patch in "${TEMP_PATCH_DIR}"/*.patch; do
    if git apply --check "${patch}" 2>/dev/null; then
        git apply "${patch}"
        echo "  [OK] $(basename "${patch}") applies cleanly."
    else
        echo "  [CONFLICT] $(basename "${patch}") does not apply cleanly to ${UPSTREAM_REF}."
        ALL_CLEAN=false
    fi
done

if [ "${ALL_CLEAN}" = true ]; then
    echo "==> SUCCESS: All RetroDrome patches apply cleanly on top of newest ${UPSTREAM_REF}!"
else
    echo "==> WARNING: Some patches require conflict resolution on newest ${UPSTREAM_REF}."
fi

trap - EXIT
cleanup

if [ "${DO_REBASE}" = "--rebase" ] && [ "${ALL_CLEAN}" = true ]; then
    echo "==> Rebasing current branch '${CURRENT_BRANCH}' onto ${UPSTREAM_REF}..."
    git rebase "${UPSTREAM_REF}"
    echo "==> Rebase completed successfully!"
elif [ "${DO_REBASE}" != "--rebase" ]; then
    echo "==> To automatically rebase your branch on top of these upstream commits, run:"
    echo "    ./scripts/sync-upstream.sh ${UPSTREAM_BRANCH} --rebase"
fi
