# TeTi IPTV Application Notes

## Purpose
Fast, ad-free IPTV player for phones, tablets and Android TV. The app ships empty:
users add their own Xtream/M3U sources. Never bundle playlists, channel logos or
mention third-party brands in store metadata.

## Architecture & Tech Stack
- Package: `com.teti.iptv`
- MVVM with Hilt, Flow/StateFlow, Jetpack Compose (Material 3)
- Room (Paging 3 sources, FTS4 search tables, explicit migrations — never rely on
  destructive migration: it would wipe users' profiles)
- OkHttp, Retrofit, kotlinx.serialization, Coil 3
- Media3 (ExoPlayer) + FFmpeg decoder (LGPL), Cast
- WorkManager: EPG refresh, TMDB enrichment
- Streaming parsers (XmlPullParser / JsonReader, batches of 500)

## Build & Test
- `./gradlew assembleDebug testDebugUnitTest`
- `./gradlew assembleRelease` runs R8 and lintVital; check it after touching models
  that are serialized (keep rules live in `app/proguard-rules.pro`).
