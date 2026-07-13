# Sarrows ProGuard / R8 rules

# ── Keep application class & Hilt ─────────────────────────────────────────────
-keep class com.sarrows.app.** { *; }
-keepclassmembers class com.sarrows.app.** { *; }

# ── Hilt generated classes ────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ── Kotlin serialization ──────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── OkHttp ────────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── Coil ──────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }

# ── Media3 / ExoPlayer ────────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Native library (JNI) ──────────────────────────────────────────────────────
-keep class com.sarrows.app.data.native.NativeSecurity {
    native <methods>;
    public *;
}

# ── Jetpack Compose ───────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── AndroidX / Lifecycle ──────────────────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }

# ── Remove logging in release ─────────────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
