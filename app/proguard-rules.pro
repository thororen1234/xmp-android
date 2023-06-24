# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep public class org.helllabs.android.xmp.Xmp {
    public *;
}

-keep public class org.helllabs.android.xmp.util.ModInfo {
    public *;
}

-keep public class * extends android.app.Application

# Keep source file and line number information in crash reports
-renamesourcefileattribute MyApplication
-keepattributes SourceFile,LineNumberTable

# For groundy

-keepattributes *Annotation*

-keepclassmembers,allowobfuscation class * {
    @com.telly.groundy.annotations.* *;
    <init>();
}

-keepnames class com.telly.groundy.generated.*
-keep class com.telly.groundy.generated.*
-keep class com.telly.groundy.ResultProxy
-keepnames class * extends com.telly.groundy.ResultProxy
-keep class * extends com.telly.groundy.GroundyTask
