# RetroDrome - Export Feature Commits to patches/ (PowerShell)
param (
    [string]$BaseRef = "upstream/dev"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$patchDir = Join-Path $rootDir "patches"

Set-Location $rootDir

Write-Host "==> Exporting commits on top of $BaseRef to $patchDir..." -ForegroundColor Cyan

Get-ChildItem -Path $patchDir -Filter "*.patch" -ErrorAction SilentlyContinue | Remove-Item -Force

git format-patch -o $patchDir "$BaseRef..HEAD"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to export patches from $BaseRef"
    exit $LASTEXITCODE
}

Write-Host "==> Patches exported successfully:" -ForegroundColor Green
Get-ChildItem -Path $patchDir -Filter "*.patch" | Format-Table Name, Length -AutoSize
