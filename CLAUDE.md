# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

Android WebView shell app — packages 1–4 URLs into a tabbed Android app via `config.json`. Single-module project with no tests, no server code, no lint.

## Build Commands

```bash
# First time only — generates gradle wrapper (Gradle 8.4)
bash setup-wrapper.sh

# Debug build
./gradlew assembleDebug

# Release build (ProGuard + shrinkResources enabled)
./gradlew assembleRelease

# Install debug APK to connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

APK output: `app/build/outputs/apk/{debug,release}/`

No test suite or lint commands exist. CI (`.github/workflows/build.yml`) only builds both variants and creates GitHub Releases on `v*` tags.

## Tech Stack

- Kotlin 1.9.22, Gradle 8.4 (KTS), AGP 8.2.2
- compileSdk 34, minSdk 24, targetSdk 34, JDK 17
- Dependencies: AndroidX (ViewPager2, WebKit, SwipeRefreshLayout), Material
- JSON parsing: `org.json.JSONObject` (no Moshi/Gson)

## Architecture

Single package `com.mimokotlin.app`, no sub-packages. All source lives in `app/src/main/java/com/mimokotlin/app/`.

**Activity flow:**
- `SplashActivity` → loads hardcoded URL (`aistudio.xiaomimimo.com/#/c`) with min 1200ms display, then transitions to `MainActivity`
- `MainActivity` → reads `config.json`, builds BottomNavigationView + ViewPager2 with `WebViewFragment` per tab

**WebViewFragment** — the core component:
- Full WebView config (JS, DOM storage, cookies, zoom, mixed content)
- File upload via `ActivityResultContracts.GetMultipleContents`
- File download via Android `DownloadManager`
- Domain whitelist: `xiaomimimo.com`, `mi.com`, `xiaomi.com` — external links trigger a dialog with 10s auto-dismiss countdown
- `injectHideButtons()` injects CSS on page load to hide specific UI elements in the web content
- Back press priority: WebView history → previous tab → exit

## Key Conventions

- **Config-driven tabs**: Edit `app/src/main/assets/config.json` to change tabs (label, icon emoji, url). Tab count is dynamic (1–4).
- **Tab icons**: Hardcoded in `MainActivity.kt` (`icons` array) — uses system drawables, NOT the `icon` field from config.json
- **Splash URL**: Hardcoded in `SplashActivity.kt:67`, not from config
- **ProGuard**: Rules in `app/proguard-rules.pro` preserve `@JavascriptInterface` methods and the entire app package
- **Cleartext traffic**: `usesCleartextTraffic="true"` in manifest — required for HTTP sites
- **No DI, no ViewModel, no coroutines** — straightforward Activity/Fragment/Android framework code

## When Modifying

| What to change | Where |
|---|---|
| Tab URLs/labels | `app/src/main/assets/config.json` |
| Tab icons | `MainActivity.kt` (`icons` array) |
| Splash screen URL | `SplashActivity.kt:67` |
| Domain whitelist | `WebViewFragment.kt` (`internalDomains` list) |
| Hidden web UI elements | `WebViewFragment.kt` (`injectHideButtons()` JS) |
| ProGuard rules | `app/proguard-rules.pro` |
