# TeTi IPTV Application Notes

## Purpose
High performance native Android IPTV app blending Netflix aesthetic with TiviMate speed.

## Architecture & Tech Stack
- Package: `com.teti.iptv`
- Clean Architecture + MVVM (Hilt, Flow, StateFlow)
- Room Database (FTS4 full text search, indexing, Paging 3 source)
- OkHttp, Retrofit, Kotlinx Serialization, Coil 3
- AndroidX Media3 (ExoPlayer) with OkHttpDataSource, PiP, multi-track audio/subtitles
- WorkManager background periodic sync for playlists and EPG
- Streaming XmlPullParser & JsonReader (batch size 500) for zero OOM

## Build & Test
- `./gradlew assembleDebug` or `gradle assembleDebug`
