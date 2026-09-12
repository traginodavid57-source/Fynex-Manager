# ProGuard rules for Fynex
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-dontwarn net.lingala.zip4j.**
-dontwarn com.topjohnwu.superuser.**
