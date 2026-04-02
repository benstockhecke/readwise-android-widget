# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.readwise.widget.api.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Tink / EncryptedSharedPreferences
-dontwarn com.google.errorprone.annotations.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.readwise.widget.**$$serializer { *; }
-keepclassmembers class com.readwise.widget.** { *** Companion; }
-keepclasseswithmembers class com.readwise.widget.** { kotlinx.serialization.KSerializer serializer(...); }
