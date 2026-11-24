# Proguard rules for TeTi
-keepattributes *Annotation*
-dontwarn okio.**
-dontwarn coil3.**

# Enum adları SharedPreferences ve Room'da metin olarak saklanır (valueOf ile okunur).
-keepclassmembers enum com.teti.iptv.** { *; }

# kotlinx.serialization: modellerin serializer'ları (kütüphane kurallarına ek güvence).
-keepclassmembers @kotlinx.serialization.Serializable class com.teti.iptv.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit arayüzleri yansıma ile oluşturulur.
-keep,allowobfuscation interface com.teti.iptv.data.remote.** { *; }

# Cast seçenek sağlayıcısı manifestten sınıf adıyla yüklenir.
-keep class androidx.media3.cast.DefaultCastOptionsProvider { *; }
