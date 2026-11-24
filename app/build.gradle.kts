import java.util.Properties

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
        versionCode = 5
        versionName = "1.2.0"

        // TMDB kimlik bilgileri kaynak kodda tutulmaz: ortam değişkeni (CI secret) ya da
        // local.properties'ten okunur. Boşsa kullanıcı Ayarlar'dan kendi anahtarını girer.
        val tmdbApiKey = secretOrLocal("TETI_TMDB_API_KEY", "tmdb.apiKey")
        val tmdbReadToken = secretOrLocal("TETI_TMDB_READ_TOKEN", "tmdb.readToken")
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
            // R8: kullanılmayan kod ve kaynakları atar, kalanı optimize eder.
            isMinifyEnabled = true
            isShrinkResources = true
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
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Robolectric'in Android imajını da aynadan indir (Maven Central 429 verebiliyor).
            it.systemProperty(
                "robolectric.dependency.repo.url",
                "https://maven-central.storage-download.googleapis.com/maven2"
            )
        }
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

    implementation(libs.androidx.profileinstaller)
    implementation(libs.media3.cast)

    implementation(libs.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

fun secretOrLocal(envName: String, propertyName: String): String {
    System.getenv(envName)?.takeIf { it.isNotBlank() }?.let { return it }
    val file = rootProject.file("local.properties")
    if (!file.exists()) return ""
    val props = Properties().apply { file.inputStream().use { load(it) } }
    return props.getProperty(propertyName).orEmpty()
}
