# KA POS — ProGuard Rules untuk Release Build
# Aturan ini memastikan library yang digunakan tidak ter-strip saat minifikasi

# ============================================================
# Room Database
# ============================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}
-dontwarn androidx.room.**

# ============================================================
# Kotlinx Serialization
# ============================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class * {
    *;
}
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================
# OkHttp3
# ============================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ============================================================
# Jetpack Compose
# ============================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ============================================================
# Kotlin
# ============================================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ============================================================
# App Domain Models — jangan di-obfuscate karena dipakai Serialization
# ============================================================
-keep class com.kapos.mobile.domain.model.** { *; }
-keep class com.kapos.mobile.core.data.local.entity.** { *; }
-keep class com.kapos.mobile.core.data.remote.dto.** { *; }
-keep class com.kapos.mobile.core.data.repository.AppSettings { *; }

# ============================================================
# Enums — jaga agar valueOf() tetap bekerja
# ============================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
