# RetroDrome - Apply Patch Layer onto Upstream RetroMusic (PowerShell)
param (
    [switch]$CheckOnly
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$patchDir = Join-Path $rootDir "patches"

Set-Location $rootDir

$patches = Get-ChildItem -Path $patchDir -Filter "*.patch" | Sort-Object Name
if ($patches.Count -eq 0) {
    Write-Error "No patch files found in $patchDir"
    exit 1
}

Write-Host "==> Validating $($patches.Count) patches against current tree..." -ForegroundColor Cyan
foreach ($patch in $patches) {
    Write-Host "  -> Checking $($patch.Name)..."
    git apply --check $patch.FullName
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Validation failed for $($patch.Name). Make sure working tree is on clean compatible upstream commit."
        exit $LASTEXITCODE
    }
}

if ($CheckOnly) {
    Write-Host "==> All patches validated successfully (check-only mode)." -ForegroundColor Green
    exit 0
}

Write-Host "==> Applying $($patches.Count) patches..." -ForegroundColor Cyan
foreach ($patch in $patches) {
    Write-Host "  -> Applying $($patch.Name)..."
    git apply $patch.FullName
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to apply $($patch.Name)"
        exit $LASTEXITCODE
    }
}

Write-Host "==> All RetroDrome patches applied successfully!" -ForegroundColor Green
