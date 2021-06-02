# Kotlin Serialization

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt # core serialization annotations

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class io.github.airvision.**$$serializer { *; }
-keepclassmembers class io.github.airvision.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.airvision.** { # <-- change package name to your app's
    kotlinx.serialization.KSerializer serializer(...);
}
