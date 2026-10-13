# RetroDrome - AI Agent & Harness Instructions

This file provides autonomous execution instructions for AI agents, coding harnesses (Antigravity, Devin, Claude Code, Cursor, Copilot, etc.), and CI automations working on **RetroDrome**.

When instructed to perform tasks like **"push updates"**, **"sync upstream"**, **"make a release"**, or **"send to phone"**, follow the exact protocols below without requiring manual user intervention.

---

## 🧭 Repository Identity & Remotes

- **Project**: RetroDrome (degoogled Android music player with seamless local + Navidrome streaming).
- **Default Branch**: `dev`
- **Remotes**:
  - `origin`: `https://github.com/Zephyr73/RetroDrome.git` (RetroDrome fork repository)
  - `upstream`: `https://github.com/RetroMusicPlayer/RetroMusicPlayer.git` (upstream RetroMusic repository)
- **Flavor & Java**:
  - JDK: Java 21
  - Flavor: Pure F-Droid (`fdroid`) — NO Google Play / proprietary dependencies.
  - Build tasks: `:app:assembleFdroidDebug` and `:app:assembleFdroidRelease`.

---

## ⚡ Automated Playbooks

### Playbook 1: "Push updates" / "Commit and push to GitHub"

When the user says *"push updates"*, *"push to github"*, or *"commit and push"*:

1. **Check Status**:
   ```bash
   git status --short
   ```
2. **Verify Build**:
   ```bash
   ./gradlew :app:assembleFdroidDebug
   ```
3. **Commit Code Changes**:
   - Stage modified code, resources, workflows, or documentation:
     ```bash
     git add app/ scripts/ .github/ README.md INSTRUCTIONS.md AGENTS.md
     git commit -m "feat: <clear descriptive message>"
     ```
4. **Re-export Clean Patch Series**:
   - Run the patch export script (this ensures `patches/` matches code commits 1:1 without circular patch bloat):
     ```powershell
     # Windows PowerShell:
     .\scripts\export-patches.ps1

     # Linux / macOS / Bash:
     ./scripts/export-patches.sh
     ```
5. **Commit Patches**:
   ```bash
   git add patches/
   git commit -m "chore: update patch series"
   ```
6. **Push to GitHub**:
   ```bash
   # If linear progression:
   git push origin dev

   # If branch was rebased:
   git push --force-with-lease origin dev
   ```

---

## 🏷️ Versioning Protocol for AI Harnesses & Automations

RetroDrome uses an explicit **dual versioning system** to keep upstream lineage transparent while providing independent semantic versioning for RetroDrome:

### 1. Version Components in `app/build.gradle.kts`
```kotlin
val retrodromeVersion = "1.0.0"       // RetroDrome Semantic Version (MAJOR.MINOR.PATCH)
val upstreamRetroVersion = "7.0.3"     // Base upstream RetroMusic tag / version
versionName = "$retrodromeVersion (based on $upstreamRetroVersion)"
versionCode = 10703                    // Android version code
```

### 2. When to Increment Each Component
- **Bump `retrodromeVersion` PATCH (`1.0.0` -> `1.0.1`)**:
  - Bug fixes (e.g. playback errors, sync glitch, date formatters, lint issues).
  - Minor UI styling adjustments.
- **Bump `retrodromeVersion` MINOR (`1.0.0` -> `1.1.0`)**:
  - New backwards-compatible features (e.g. Subsonic lyrics, server playlists, offline caching, new widgets).
  - Reset PATCH to 0.
- **Bump `retrodromeVersion` MAJOR (`1.0.0` -> `2.0.0`)**:
  - Architectural overhauls or database schema migrations that break backwards compatibility.
  - Reset MINOR and PATCH to 0.
- **Bump `upstreamRetroVersion` (`7.0.3` -> `7.0.4`)**:
  - ONLY incremented when rebasing RetroDrome onto a new upstream RetroMusic release.
  - Bump `versionCode` to match upstream or add appropriate build offset.

### 3. Git Tag Format
- Always prefix with `v` followed by the RetroDrome version: **`v<retrodromeVersion>`** (e.g. `v1.0.0`, `v1.0.1`).
- **DO NOT** use the upstream version for the Git tag (e.g. do NOT tag `v7.0.3` for a RetroDrome release).

---

### Playbook 2: "Make a release" / "Publish release"

When the user says *"make a release"*, *"release vX.Y.Z"*, or *"cut a release"*:

1. **Verify or Bump Version** in `app/build.gradle.kts`:
   - Set `retrodromeVersion = "X.Y.Z"`
   - Ensure `versionName = "$retrodromeVersion (based on $upstreamRetroVersion)"`
2. **Local Pre-flight Verification**:
   - Run both lint and release compilation:
     ```bash
     ./gradlew lint
     ./gradlew :app:assembleFdroidRelease
     ```
   - Verify output APK exists at: `app/build/outputs/apk/fdroid/release/app-fdroid-release.apk`
3. **Commit Code & Re-export Clean Patches**:
   ```bash
   git add app/build.gradle.kts INSTRUCTIONS.md
   git commit -m "chore: bump version to X.Y.Z"
   ./scripts/export-patches.sh   # or .\scripts\export-patches.ps1
   git add patches/
   git commit -m "chore: update patch series for vX.Y.Z"
   ```
4. **Push Branch & Push Annotated Tag**:
   ```bash
   git push origin dev
   git tag -a vX.Y.Z -m "RetroDrome Release vX.Y.Z (based on <upstreamRetroVersion>)"
   git push origin vX.Y.Z
   ```
5. **Monitor GitHub Release CI**:
   - Pushing tag `v*` automatically triggers `.github/workflows/release.yml`.
   - The workflow compiles the release APK, generates release notes from commits, and publishes the GitHub Release with `app-fdroid-release.apk` attached.

---

### Playbook 3: "Sync upstream" / "Pull bugfixes from Retro"

When the user says *"sync upstream"*, *"pull latest bugfixes"*, or *"check upstream updates"*:

1. **Run Upstream Sync Script**:
   ```powershell
   # Windows PowerShell:
   .\scripts\sync-upstream.ps1

   # Linux / macOS:
   ./scripts/sync-upstream.sh
   ```
2. **If Bugfixes Are Available & Patches Apply Cleanly**:
   - Rebase automatically:
     ```powershell
     .\scripts\sync-upstream.ps1 -Rebase
     # or: ./scripts/sync-upstream.sh dev --rebase
     ```
3. **If Upstream Conflicts Occur**:
   - The script will flag exactly which patch conflicted (`[CONFLICT]`).
   - Resolve conflicts in the code.
   - Re-export patches via `.\scripts\export-patches.ps1`.
   - Rebuild and test: `./gradlew :app:assembleFdroidDebug`.
4. **Push Synced History**:
   ```bash
   git push --force-with-lease origin dev
   ```

---

### Playbook 4: "Send to phone" / Wireless ADB Deployment

When the user says *"wirelessly send build to my phone"* or provides an ADB IP:Port (e.g. `100.68.1.201:42067`):

1. **Connect ADB**:
   ```bash
   adb connect <IP>:<PORT>
   adb devices
   ```
2. **Compile Release or Debug APK**:
   ```bash
   ./gradlew :app:assembleFdroidRelease
   ```
3. **Install on Device**:
   ```bash
   adb install -r app/build/outputs/apk/fdroid/release/app-fdroid-release.apk
   ```
4. **Report Details to User**:
   - Package: `code.name.monkey.retromusic`
   - Version Name: `1.0.0 (based on 7.0.3)`
   - Status: Success / installed.

---

## 🛡️ Critical Architecture & Hygiene Rules

1. **Dual Versioning System**:
   - Always maintain the dual version format: `Version <RetroDrome> (based on <Upstream>)`.
   - Never overwrite or erase `BuildConfig.RETRODROME_VERSION` or `BuildConfig.UPSTREAM_RETRO_VERSION`.
2. **Never Edit Patches Manually**:
   - Never make hand edits to files inside `patches/`.
   - Always modify app code, verify compilation, then use `export-patches.ps1` / `export-patches.sh`.
   - The export scripts automatically exclude `patches/` (`-- . ':!patches'`) to prevent recursive meta-patch bloat.
3. **Clean Upstream Patched Build**:
   - To test building directly on clean upstream without contaminating the current branch:
     ```powershell
     .\scripts\build-patched-apk.ps1 -BuildType Release
     ```
4. **No Proprietary Dependencies**:
   - Keep the project pure F-Droid compatible. Do not introduce Play Services, Play Billing, or Google Cast into `main` or `fdroid`.
5. **Background Threading**:
   - Room, Subsonic network calls, MediaStore scans, and tag operations must remain off the main thread (`Dispatchers.IO`).
