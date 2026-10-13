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

$status = git status --porcelain
if ($status) {
    Write-Error "Working directory has uncommitted changes. Please commit or stash before running this build script."
    exit 1
}

$patches = Get-ChildItem -Path $patchDir -Filter "*.patch" | Sort-Object Name
if ($patches.Count -eq 0) {
    Write-Error "No patch files found in $patchDir"
    exit 1
}

Write-Host "==> Staging $($patches.Count) patches into temporary storage..." -ForegroundColor Cyan
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$tempPatchDir = Join-Path ([System.IO.Path]::GetTempPath()) "retrodrome-patches-$timestamp"
New-Item -ItemType Directory -Force -Path $tempPatchDir | Out-Null
Copy-Item (Join-Path $patchDir "*.patch") -Destination $tempPatchDir

Write-Host "==> Fetching upstream..." -ForegroundColor Cyan
git fetch upstream 2>&1 | Out-Null

$buildBranch = "retrodrome-build-temp-$timestamp"
$currentBranch = (git branch --show-current).Trim()

try {
    Write-Host "==> Checking out clean base: $BaseRef..." -ForegroundColor Cyan
    git checkout --quiet -b $buildBranch $BaseRef
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to checkout base ref $BaseRef"
    }

    $stagedPatches = Get-ChildItem -Path $tempPatchDir -Filter "*.patch" | Sort-Object Name
    Write-Host "==> Applying $($stagedPatches.Count) RetroDrome patches..." -ForegroundColor Cyan
    foreach ($patch in $stagedPatches) {
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
    git clean -fd 2>&1 | Out-Null
    git checkout --quiet $currentBranch 2>&1 | Out-Null
    git branch -D $buildBranch 2>&1 | Out-Null
    if (Test-Path $tempPatchDir) {
        Remove-Item -Recurse -Force $tempPatchDir 2>&1 | Out-Null
    }
}
