# Keep Kotlin Coroutines & DataStore
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }
-keep class kotlinx.coroutines.** { *; }

# Keep Compose Runtime & Material
-keep class androidx.compose.** { *; }

# Keep WebKit Javascript Interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve Android Components & Services
-keep class com.socialbreak.android.services.** { *; }
-keep class com.socialbreak.android.receivers.** { *; }
-keep class com.socialbreak.android.data.** { *; }
