# RetroDrome - Sync & Validate Upstream RetroMusic Bugfixes (PowerShell)
param (
    [string]$UpstreamBranch = "dev",
    [switch]$Rebase
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$patchDir = Join-Path $rootDir "patches"

Set-Location $rootDir

Write-Host "==> Checking git remotes..." -ForegroundColor Cyan
$remotes = git remote
if ($remotes -notcontains "upstream") {
    Write-Host "  -> Adding upstream remote (RetroMusicPlayer/RetroMusicPlayer)..." -ForegroundColor Yellow
    git remote add upstream https://github.com/RetroMusicPlayer/RetroMusicPlayer.git
}

Write-Host "==> Fetching latest changes from upstream..." -ForegroundColor Cyan
git fetch upstream

$upstreamRef = "upstream/$UpstreamBranch"
$baseRef = (git merge-base HEAD $upstreamRef).Trim()
$upstreamHead = (git rev-parse $upstreamRef).Trim()

if ($baseRef -eq $upstreamHead) {
    Write-Host "==> Up to date! Current branch already includes all commits from $upstreamRef." -ForegroundColor Green
    exit 0
}

$newCommitCount = (git rev-list --count "$baseRef..$upstreamRef").Trim()
Write-Host "==> Found $newCommitCount new commit(s) / bugfix(es) in ${upstreamRef}:" -ForegroundColor Yellow
git log --oneline -n 10 "$baseRef..$upstreamRef"

Write-Host "==> Testing whether RetroDrome patches apply cleanly on top of newest $upstreamRef..." -ForegroundColor Cyan
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$tempPatchDir = Join-Path ([System.IO.Path]::GetTempPath()) "retrodrome-sync-patches-$timestamp"
New-Item -ItemType Directory -Force -Path $tempPatchDir | Out-Null
Copy-Item (Join-Path $patchDir "*.patch") -Destination $tempPatchDir

$testBranch = "retrodrome-sync-test-$timestamp"
$currentBranch = (git branch --show-current).Trim()

try {
    git checkout --quiet -b $testBranch $upstreamRef
    $stagedPatches = Get-ChildItem -Path $tempPatchDir -Filter "*.patch" | Sort-Object Name
    $allClean = $true
    foreach ($patch in $stagedPatches) {
        git apply --check $patch.FullName 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  [CONFLICT] $($patch.Name) does not apply cleanly to $upstreamRef." -ForegroundColor Red
            $allClean = $false
        } else {
            git apply $patch.FullName
            Write-Host "  [OK] $($patch.Name) applies cleanly." -ForegroundColor Green
        }
    }

    if ($allClean) {
        Write-Host "==> SUCCESS: All RetroDrome patches apply cleanly on top of newest $upstreamRef!" -ForegroundColor Green
    } else {
        Write-Host "==> WARNING: Some patches require conflict resolution on newest $upstreamRef." -ForegroundColor Yellow
    }
}
finally {
    git reset --hard 2>&1 | Out-Null
    git clean -fd 2>&1 | Out-Null
    git checkout --quiet $currentBranch 2>&1 | Out-Null
    git branch -D $testBranch 2>&1 | Out-Null
    if (Test-Path $tempPatchDir) {
        Remove-Item -Recurse -Force $tempPatchDir 2>&1 | Out-Null
    }
}

if ($Rebase -and $allClean) {
    Write-Host "==> Rebasing current branch '$currentBranch' onto $upstreamRef..." -ForegroundColor Cyan
    git rebase $upstreamRef
    Write-Host "==> Rebase completed successfully!" -ForegroundColor Green
} elseif (-not $Rebase) {
    Write-Host "==> To automatically rebase your branch on top of these upstream commits, run:" -ForegroundColor Cyan
    Write-Host "    .\scripts\sync-upstream.ps1 -Rebase" -ForegroundColor Gray
}
