# Sarrows — Android Native App

Streaming app for movies, anime & series. Native Android app built with Kotlin + Jetpack Compose + Android NDK (C/C++) + CMake.

## Web version
https://sarrows.vercel.app (API base URL)

## Tech stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3 (dark streaming theme)
- **Security layer**: Android NDK (C/C++) via JNI — handles cookie storage, CSRF tokens, URL building, request signing
- **Native library**: `sarrows_native` (libsarrows_native.so)
- **Networking**: OkHttp (Kotlin) — headers/cookies injected from C++ layer
- **Video**: Media3 ExoPlayer (HLS + direct MP4/WebM) + WebView (embed type)
- **DI**: Hilt
- **Navigation**: Navigation Compose
- **Image loading**: Coil
- **Serialization**: Kotlinx Serialization
- **Build**: Gradle 8.7 + AGP 8.5.2
- **CI/CD**: Codemagic (codemagic.yaml)

## Project structure
```
app/src/main/
├── cpp/                    ← C++ NDK security layer
│   ├── CMakeLists.txt
│   ├── sarrows_jni.cpp     ← JNI bridge (30+ native methods)
│   ├── cookie_manager.cpp/h ← Encrypted in-memory cookie store
│   ├── api_security.cpp/h  ← CSRF, header building, URL helpers, rate limiting
│   └── crypto_utils.cpp/h  ← XOR cipher, key derivation, secure clear
├── java/com/sarrows/app/
│   ├── SarrowsApp.kt       ← Hilt application class
│   ├── MainActivity.kt     ← Entry point, splash screen
│   ├── data/
│   │   ├── models/Models.kt        ← All data models + API result sealed class
│   │   ├── native/NativeSecurity.kt ← Kotlin JNI wrapper
│   │   ├── remote/SarrowsApiClient.kt ← Full API client (all 17 sections)
│   │   └── repository/SarrowsRepository.kt
│   ├── di/AppModule.kt             ← Hilt DI module
│   └── ui/
│       ├── theme/            ← Dark streaming theme (Color, Type, Theme)
│       ├── navigation/       ← Navigation graph
│       ├── components/       ← ContentCard, BottomNavBar, SectionHeader
│       ├── viewmodels/       ← Auth, Home, Browse, Search, Detail, Profile, Requests, Player
│       ├── player/PlayerActivity.kt ← Fullscreen player (ExoPlayer + WebView)
│       └── screens/
│           ├── auth/         ← Login, SignUp
│           ├── home/         ← Home (hero + sections)
│           ├── browse/       ← Movies / Anime / Series grid with filters
│           ├── search/       ← Search with debounce
│           ├── detail/       ← Movie/Series detail (cast, reviews, watchlist)
│           ├── profile/      ← Profile + menu
│           └── requests/     ← Content request submit/view/cancel
```

## Authentication flow (from API doc)
1. `GET /api/auth/csrf` → get CSRF token (C++ stores it)
2. `POST /api/auth/callback/credentials` form-encoded (body built in C++)
3. Session cookie `authjs.session-token` stored encrypted in C++ cookie store
4. Every subsequent request has Cookie header injected from C++ layer

## Security architecture
- **C++ layer** (NDK): cookie encryption (XOR keystream + Android Keystore-derived key), CSRF management, URL obfuscation, rate-limit tracking, header building, login body construction
- **Kotlin layer**: actual HTTP I/O (OkHttp), UI, ExoPlayer
- Cookie plaintext never touches Kotlin memory — encrypted immediately in C++

## Playback resolution strategy (from API doc §9.6)
1. Try `GET /api/stream/movie/:id` → if 200, ExoPlayer plays the stream URL
2. If 400 → try `GET /api/stream/movie/:id/embed` → if 200, load URL in WebView

## Build (Codemagic)
- **Debug**: push to `main` or `develop` → `assembleDebug`
- **Release**: tag `v*` → `assembleRelease` + `bundleRelease`
- Configure keystore as `sarrows_keystore` in Codemagic Code Signing

## Setup steps for first build
1. In Codemagic UI → create new project → connect this repo
2. Add `sarrows_keystore` under Code Signing → Android
3. Set `GCLOUD_SERVICE_ACCOUNT_CREDENTIALS` env var for Play Store uploads (optional)
4. Set workflow to `android-debug` or `android-release`
5. Trigger build — Codemagic handles NDK/CMake compilation automatically

### gradle-wrapper.jar (required for local builds)
The binary `gradle/wrapper/gradle-wrapper.jar` is not included in git (binary). To generate it:
```sh
gradle wrapper --gradle-version=8.7
```
Or download from: https://services.gradle.org/distributions/
Codemagic automatically handles this — no manual step needed for CI builds.

## User preferences
- API calls and security in C/C++ for security
- Build APK using Codemagic
- Dark streaming theme
- Kotlin + Jetpack Compose + Android NDK + CMake
