# Proguard / R8 configuration for NSync Release APK

# Keep Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    companion <fields>;
}
-keep class kotlinx.serialization.** { *; }

# Keep App Models & Data Classes
-keep class com.example.nsutconnect.data.model.** { *; }
-keep class com.example.nsutconnect.data.network.** { *; }
-keep class com.example.nsutconnect.data.update.** { *; }
-keep class com.example.nsutconnect.data.sync.** { *; }
-keep class com.example.nsutconnect.data.attendance.** { *; }
-keep class com.example.nsutconnect.data.schedule.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }

# Coil Image Loading
-keep class coil.** { *; }

# AndroidX Navigation & Compose
-keep class androidx.navigation.** { *; }
-keep class androidx.compose.** { *; }

# AndroidX Startup, WorkManager & Room
-keep class androidx.startup.** { *; }
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.work.**
-dontwarn androidx.room.**
