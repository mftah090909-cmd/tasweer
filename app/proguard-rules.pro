# LUMIX PRO - R8 Proguard Rules

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# AndroidX Exif
-keep class androidx.exifinterface.media.** { *; }

# Preserve Compose compiler generated code
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
