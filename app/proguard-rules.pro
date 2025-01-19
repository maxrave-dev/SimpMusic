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
-keep class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlinx.coroutines.internal.MainDispatcherFactory
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn kotlinx.serialization.internal.ClassValueReferences
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.maxrave.simpmusic.data.model.** { *; }
-keep class com.maxrave.simpmusic.extension.AllExtKt { *; }
-keep class com.maxrave.simpmusic.extension.AllExtKt$* { *; }
-keep class com.maxrave.kotlinytmusicscraper.extension.MapExtKt$* { *; }

# Removes all Logs as they cause perfomance issues in prod
-assumenosideeffects class android.util.Log {
    public static int w(...);
    public static int e(...);
    public static int i(...);
    public static int d(...);
    public static int v(...);
}
## Rules for NewPipeExtractor
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
# Please add these rules to your existing keep rules in order to suppress warning
# This is generated automatically by the Android Gradle plugin.
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor