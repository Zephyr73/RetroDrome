<p align="center">
  <img src="app/src/main/ic_launcher-web.png" height="128">
  <h1 align="center">RetroDrome 🎵</h1>
</p>

<p align="center">
  <a href="https://github.com/Zephyr73/RetroDrome" style="text-decoration:none" aria-label="Android">
    <img src="https://img.shields.io/badge/Platform-Android-green.svg">
  </a>
  <a href="https://github.com/Zephyr73/RetroDrome" style="text-decoration:none" aria-label="Min API: 21">
    <img src="https://img.shields.io/badge/minSdkVersion-21-green.svg">
  </a>
  <a href="https://github.com/Zephyr73/RetroDrome/blob/master/LICENSE.md" style="text-decoration:none" aria-label="License: GPL v3">
    <img src="https://img.shields.io/badge/License-GPL%20v3-blue.svg">
  </a>
  <a href="https://f-droid.org" style="text-decoration:none" aria-label="Build: F--Droid">
    <img src="https://img.shields.io/badge/Build-Pure%20F--Droid-brightgreen.svg">
  </a>
</p>

___

## 🌟 About RetroDrome

**RetroDrome** is an open-source, de-googled fork of the beloved [Retro Music Player](https://github.com/RetroMusicPlayer/RetroMusicPlayer), enhanced with native support for **Navidrome** / Subsonic music servers and an intelligent, unified music database.

> [!NOTE]
> **Fork & AI-Assisted Architecture**:  
> RetroDrome was engineered and architected with the assistance of **AI** (Google DeepMind's Antigravity). Rather than maintaining a messy hard fork that drifts from upstream bug fixes, RetroDrome is built as a clean, modular patch series (`patches/`) on top of upstream RetroMusic. This allows the app to seamlessly pull and apply upstream improvements while keeping its Navidrome streaming and unified catalog capabilities intact.

___

## ✨ What Makes RetroDrome Special?

- 🌐 **Native Navidrome / Subsonic Integration**: Stream your personal music collection directly from your self-hosted Navidrome or Subsonic-compatible server with secure token-based authentication.
- 💿 **Seamless Unified Music Catalog**: Local device music (via Android MediaStore) and Navidrome server music are synchronized into a persistent Room database. Tracks, albums, and artists found in both places are automatically merged into single entries—no disjointed views or split libraries.
- ⚡ **Intelligent Dual-Path Playback**: When playing tracks that exist both locally and on the server, RetroDrome prioritizes your local file using device hardware decoding (`c2.sec.mp3.decoder`), giving you zero network latency and zero mobile data usage. Remote-only tracks stream smoothly over HTTP.
- 🛡️ **Pure De-Googled F-Droid Experience**: All proprietary Google Play Billing, Cast, and closed-source dependencies have been removed in favor of a clean, battery-friendly, privacy-focused build.
- 🔄 **Automated Upstream Patch Pipeline**: Built-in scripts (`sync-upstream`, `apply-patches`, `export-patches`) allow you to rebase onto upstream RetroMusic updates at any time.

___

## 📱 Screenshots

### App Themes
| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" width="200"/> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg" width="200"/> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg" width="200"/> |
|:---:|:---:|:---:|
|Clearly white| Kinda dark | Just black (AMOLED)|

### Player Screen
| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" width="200"/>| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.jpg" width="200"/>| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.jpg" width="200"/>| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.jpg" width="200"/>| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/8.jpg" width="200"/>|
|:---:|:---:|:---:|:---:|:---:|
| Home | Songs | Albums | Artists | Settings |

### Synced Lyrics (Over Cover & Replace Cover)
| <img src="screenshots/synced_over_light.jpg" width="200"/>| <img src="screenshots/synced_over_dark.jpg" width="200"/>| <img src="screenshots/synced_over_black.jpg" width="200"/>|
|:---:|:---:|:---:|
| Synced Light | Synced Dark | Synced Black |

### 10+ Now Playing Themes
| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" width="200"/>	|<img src="screenshots/fit.jpg" width="200"/>|   <img src="screenshots/flat.jpg" width="200"/>  	|    <img src="screenshots/color.jpg" width="200"/> 	|     <img src="screenshots/material.jpg" width="200"/>	|
|:-----:	|:-----:	|:-----:	|:-----:	|:-----:	|
| Normal 	| Fit 	| Flat 	| Color 	| Material 	|

| <img src="screenshots/classic.jpg" width="200"/>	|<img src="screenshots/adaptive.jpg" width="200"/>|   <img src="screenshots/blur.jpg" width="200"/>  	|    <img src="screenshots/tiny.jpg" width="200"/> 	|     <img src="screenshots/peek.jpg" width="200"/>	|
|:-----:	|:-----:	|:-----:	|:-----:	|:-----:	|
| Classic 	| Adaptive 	| Blur 	| Tiny 	| Peek 	|

___

## 📦 Features Overview

- **Unified Library**: Browse all local and Navidrome tracks, albums, artists, and playlists side by side.
- **Navidrome Settings**: Quick server configuration with URL, username, password/token, and connection verification in Settings.
- **Material You Design**: Dynamic accent colors on Android 12+ and Monet themed icon support on Android 13+.
- **3 Base Themes**: Clearly White, Kinda Dark, and Just Black for AMOLED displays.
- **10+ Now Playing Themes**: Choose your favorite player layout and visual style.
- **Synced & Plain Lyrics**: Embedded lyrics and synchronized scrolling.
- **Smart Playlists**: Recently played, top played, and listening history.
- **Offline & Low-Bandwidth Friendly**: Cached album covers, local playback prioritization, and efficient Glide image loading.
- **Gapless Playback**: Uninterrupted audio playback across tracks.
- **Sleep Timer & Driving Mode**: Built-in audio tools for bedtime and commute.
- **Tag Editor & Metadata Viewer**: Edit track details and view detailed audio metadata.
- **Home Screen & Lock Screen Controls**: Rich notification and widget controls.

___

## 🛠️ Building & Maintenance Workflow

### Building the APK

RetroDrome targets Java 21 and the pure F-Droid build flavor:

```bash
# Debug build (fast local iteration):
./gradlew :app:assembleFdroidDebug

# Release build:
./gradlew :app:assembleFdroidRelease
```

### Versioning Scheme

RetroDrome follows an explicit **dual versioning system**:
- **RetroDrome Version**: Tracks feature releases and fixes specific to RetroDrome (e.g. `1.0.0`).
- **Upstream Retro Version**: Identifies the base upstream release (e.g. `7.0.3`).
- **Display Format**: `Version 1.0.0 (based on 7.0.3)`.

### Automation & Upstream Sync Workflow

RetroDrome includes automated scripts and GitHub Actions pipelines to pull upstream Retro bugfixes, validate patches, and create releases:

```bash
# Fetch latest upstream Retro bugfixes and test patch compatibility:
.\scripts\sync-upstream.ps1           # Windows PowerShell
./scripts/sync-upstream.sh             # Linux / macOS

# Build a clean F-Droid APK directly on top of upstream with patches applied:
.\scripts\build-patched-apk.ps1 -BuildType Release
./scripts/build-patched-apk.sh upstream/dev Release

# Validate or re-apply patches on a clean tree:
.\scripts\apply-patches.ps1
./scripts/apply-patches.sh

# Export newly created feature commits to patches/:
.\scripts\export-patches.ps1
./scripts/export-patches.sh
```

### GitHub Actions Automation
- **`upstream-sync-and-build.yml`**: Automatically checks out upstream Retro weekly (or on demand), applies all RetroDrome patches, builds the release APK, and uploads it as an artifact.
- **`release.yml`**: Automatically triggered on tag push (or manual dispatch), builds the release APK, signs it, and publishes a GitHub Release with the APK attached.

___

## 🗂️ License & Credits

RetroDrome is released under the **GNU General Public License v3.0 (GPLv3)**, which can be found in [LICENSE.md](LICENSE.md).

- **Original Project**: Huge thanks to the [Retro Music Player](https://github.com/RetroMusicPlayer/RetroMusicPlayer) team for creating such a gorgeous and intuitive Android music player.
- **Navidrome Support**: Built for seamless integration with the [Navidrome](https://www.navidrome.org/) open-source music server.
