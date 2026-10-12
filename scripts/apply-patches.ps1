# RetroDrome - Apply Patch Layer onto Upstream RetroMusic (PowerShell)
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

# Check if all patches are already applied
$allAlreadyApplied = $true
foreach ($patch in $patches) {
    git apply --check --reverse $patch.FullName 2>$null
    if ($LASTEXITCODE -ne 0) {
        $allAlreadyApplied = $false
        break
    }
}

if ($allAlreadyApplied) {
    Write-Host "==> All RetroDrome patches are already applied to the current tree." -ForegroundColor Yellow
    exit 0
}

Write-Host "==> Checking patches against current working tree..." -ForegroundColor Cyan
foreach ($patch in $patches) {
    Write-Host "  -> Validating $($patch.Name)..."
    git apply --check $patch.FullName
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Validation failed for $($patch.Name). Make sure you are on a clean upstream commit."
        exit $LASTEXITCODE
    }
}

Write-Host "==> Applying patches..." -ForegroundColor Cyan
foreach ($patch in $patches) {
    Write-Host "  -> Applying $($patch.Name)..."
    git apply $patch.FullName
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to apply $($patch.Name)"
        exit $LASTEXITCODE
    }
}

Write-Host "==> All RetroDrome patches applied successfully!" -ForegroundColor Green
