# Proguard rules for HydroDesktop
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.hydrodesktop.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
