plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.teti.iptv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.teti.iptv"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.1.0"

        // TMDB kimlik bilgileri. Kullanıcı Ayarlar'dan kendi anahtarını girerse o kullanılır.
        // CI veya yerel derlemede TETI_TMDB_API_KEY / TETI_TMDB_READ_TOKEN ile değiştirilebilir.
        val tmdbApiKey = System.getenv("TETI_TMDB_API_KEY") ?: "718b53dc3d48cedcf2ae81d8bed699a0"
        val tmdbReadToken = System.getenv("TETI_TMDB_READ_TOKEN")
            ?: "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI3MThiNTNkYzNkNDhjZWRjZjJhZTgxZDhiZWQ2OTlhMCIsIm5iZiI6MTc5NTI1Nzk3NS45NDksInN1YiI6IjZiMDE3Njc3MGZkNGU3ZjNhYzhmOGQ5ZiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.-40fvsAcovHRYUiCNYzCaSFBrAM66P7NtydgblsM2KM"
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        buildConfigField("String", "TMDB_READ_TOKEN", "\"$tmdbReadToken\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing: set TETI_KEYSTORE_FILE, TETI_KEYSTORE_PASSWORD, TETI_KEY_ALIAS and
    // TETI_KEY_PASSWORD (CI fills them from repository secrets). Without them the release
    // APK is signed with the debug key so it still installs; an unsigned APK does not.
    val releaseKeystore = System.getenv("TETI_KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("TETI_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("TETI_KEY_ALIAS")
                keyPassword = System.getenv("TETI_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.decoder.ffmpeg)
    implementation(libs.media3.ui)

    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
}
