# ============================================================
# ADAPT — ProGuard / R8 Rules
# ============================================================

# ---------- Retrofit ----------
# Keep Retrofit interfaces (used via reflection)
-keepattributes Signature
-keepattributes *Annotation*
-keep,allowobfuscation interface retrofit2.Call
-keep,allowobfuscation interface retrofit2.Response
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ---------- OkHttp ----------
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ---------- Gson ----------
# Keep Gson model classes (serialization/deserialization)
-keep class com.example.adapt.model.** { *; }
-keep class com.example.adapt.api.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**

# ---------- AndroidX / Material ----------
-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }

# ---------- Debug info for crash reports ----------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile