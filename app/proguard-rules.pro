# Proguard rules for Bonbon Bloom (Play Store Release Build)

# Google Play Billing 7.x
-keep class com.android.billingclient.api.** { *; }
-keep interface com.android.billingclient.api.** { *; }

# Google Mobile Ads (AdMob) & User Messaging Platform (UMP)
-keep public class com.google.android.gms.ads.** { public *; }
-keep public class com.google.android.ump.** { public *; }
-keepattributes *Annotation*
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.android.ump.**

# Android Jetpack Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# Moshi & Retrofit
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepattributes Signature
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Models & Data entities
-keep class com.example.data.** { *; }
-keep class com.example.game.** { *; }
-keep class com.example.payment.** { *; }
-keep class com.example.security.** { *; }
