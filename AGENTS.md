# AGENTS.md — Mimokotlin

## What this is

Android WebView shell app. Packages 1–4 URLs into a tabbed Android app via `config.json`. Single module, no tests, no server code.

## Tech stack

- Kotlin 1.9.22, Gradle 8.4 (KTS), AGP 8.2.2
- compileSdk 34, minSdk 24, JDK 17
- Dependencies: AndroidX (ViewPager2, WebKit, SwipeRefreshLayout), Material

## Build commands

```bash
# First time only — generates gradle wrapper if missing
bash setup-wrapper.sh

# Debug build
./gradlew assembleDebug

# Release build (ProGuard + shrinkResources enabled)
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/`

No test suite, lint, or typecheck commands exist. CI (`.github/workflows/build.yml`) only builds.

## Project structure

```
app/src/main/
├── assets/config.json        ← User-facing config (tabs, app name)
├── java/com/mimokotlin/app/
│   ├── SplashActivity.kt     ← Splash with hardcoded URL
│   ├── MainActivity.kt       ← Tab host + ViewPager2
│   ├── WebViewFragment.kt    ← Core WebView (file upload/download/cookies)
│   ├── TopSwipeRefreshLayout.kt
│   └── Config.kt             ← JSON config parser
├── res/                       ← Layouts, menu, values
└── AndroidManifest.xml
```

## Key conventions

- All source in single package `com.mimokotlin.app` — no sub-packages
- Config is JSON in assets, parsed by `Config.kt` with `org.json.JSONObject` (no Moshi/Gson)
- Tab count is dynamic (1–4), icons are system drawables hardcoded in `MainActivity.kt:44-49`
- WebView uses `@JavascriptInterface` — ProGuard rules must preserve these
- `SplashActivity` has a hardcoded URL (`aistudio.xiaomimimo.com`) — not from config
- External links (different host) show a dialog with 10s auto-dismiss countdown
- Back press priority: WebView history → previous tab → exit

## When modifying

- To change tabs: edit `config.json` only
- To change splash URL: edit `SplashActivity.kt:67`
- To change tab icons: edit the `icons` array in `MainActivity.kt:44-49`
- ProGuard rules in `app/proguard-rules.pro` — keep `@JavascriptInterface` methods
- `usesCleartextTraffic="true"` in manifest — required for HTTP sites
