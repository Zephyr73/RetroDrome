# RetroDrome - Clean Upstream Build with Patches Applied (PowerShell)
param (
    [string]$BaseRef = "upstream/dev",
    [string]$BuildType = "Debug" # Debug or Release
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$patchDir = Join-Path $rootDir "patches"

Set-Location $rootDir

Write-Host "==> Preparing clean build branch from $BaseRef..." -ForegroundColor Cyan
git fetch upstream 2>&1 | Out-Null

$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$buildBranch = "retrodrome-build-temp-$timestamp"

$currentBranch = (git branch --show-current).Trim()

try {
    git checkout --quiet -b $buildBranch $BaseRef 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to checkout base ref $BaseRef"
    }

    $patches = Get-ChildItem -Path $patchDir -Filter "*.patch" | Sort-Object Name
    Write-Host "==> Applying RetroDrome patches..." -ForegroundColor Cyan
    foreach ($patch in $patches) {
        Write-Host "  -> Applying $($patch.Name)..."
        git apply $patch.FullName
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to apply patch $($patch.Name)"
        }
    }

    $capitalizedType = (Get-Culture).TextInfo.ToTitleCase($BuildType.ToLower())
    Write-Host "==> Building F-Droid $capitalizedType APK..." -ForegroundColor Cyan
    ./gradlew ":app:assembleFdroid$capitalizedType"
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed"
    }

    $lowerType = $BuildType.ToLower()
    $apkPath = "app/build/outputs/apk/fdroid/$lowerType/app-fdroid-$lowerType.apk"
    if (Test-Path $apkPath) {
        Write-Host "==> SUCCESS! Output APK created at: $apkPath" -ForegroundColor Green
    } else {
        Write-Host "==> Build completed. Check app/build/outputs/apk/fdroid/ for artifacts." -ForegroundColor Yellow
    }
}
finally {
    Write-Host "==> Cleaning up temporary build branch..." -ForegroundColor Gray
    git reset --hard 2>&1 | Out-Null
    git clean -fd -e patches -e scripts 2>&1 | Out-Null
    git checkout --quiet $currentBranch 2>&1 | Out-Null
    git branch -D $buildBranch 2>&1 | Out-Null
}
