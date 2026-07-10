# Zakra TV — keep essential reflection surfaces only

-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

# Kotlin serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
# Cover EVERY @Serializable in the app (data.model AND data.update — the auto-updater's
# GhRelease/GhAsset live in data.update; if their serializers are stripped, update checks fail).
-keep,includedescriptorclasses class com.zakratv.app.**$$serializer { *; }
-keepclassmembers class com.zakratv.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.zakratv.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp / Media3
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-keep class androidx.media3.** { *; }

# Keep data models + update wire models for serialization
-keep class com.zakratv.app.data.model.** { *; }
-keep class com.zakratv.app.data.update.GhRelease { *; }
-keep class com.zakratv.app.data.update.GhAsset { *; }
