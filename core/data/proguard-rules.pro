-keep class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlinx.coroutines.internal.MainDispatcherFactory
# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Don't print notes about potential mistakes or omissions in the configuration for kotlinx-serialization classes
# See also https://github.com/Kotlin/kotlinx.serialization/issues/1900
-dontnote kotlinx.serialization.**

# Serialization core uses `java.lang.ClassValue` for caching inside these specified classes.
# If there is no `java.lang.ClassValue` (for example, in Android), then R8/ProGuard will print a warning.
# However, since in this case they will not be used, we can disable these warnings
-dontwarn kotlinx.serialization.internal.ClassValueReferences
-dontwarn org.slf4j.impl.StaticLoggerBinder
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
# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# With R8 full mode generic signatures are stripped for classes that are not kept.
-keep,allowobfuscation,allowshrinking class retrofit2.Response
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
# May be used with robolectric or deliberate use of Bouncy Castle on Android
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-keep class com.liskovsoft.** { *; }
-keep interface com.liskovsoft.** { *; }
-keep class com.eclipsesource.v8.** { *; }
-keep class com.liskovsoft.**
-keep class com.maxrave.kotlinytmusicscraper.** { *; }


## Rules for NewPipeExtractor
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**

-keep class com.maxrave.data.di.loader.LoaderKt { *; }
-keep class com.maxrave.data.mapping.MappingKt { *; }
-keep class com.maxrave.data.extension.** { *; }

-keep class com.maxrave.kotlinytmusicscraper.extension.** { *; }
-keep class com.maxrave.kotlinytmusicscraper.models.** { *; }
-keep class com.maxrave.kotlinytmusicscraper.parser.** { *; }
-keep class com.maxrave.kotlinytmusicscraper.pages.** { *; }
-keep class com.maxrave.kotlinytmusicscraper.utils.** { *; }

-keep class org.simpmusic.lyrics.parser.** { *; }
-keep class org.simpmusic.lyrics.models.** { *; }
-keep class com.simpmusic.lyrics.parser.** { *; }

-dontwarn com.maxrave.kotlinytmusicscraper.YouTube$SearchFilter$Companion
-dontwarn com.maxrave.kotlinytmusicscraper.YouTube$SearchFilter
-dontwarn com.maxrave.kotlinytmusicscraper.YouTube
-dontwarn com.maxrave.media3.di.Media3ServiceModuleKt
-dontwarn com.maxrave.media3.exoplayer.ExoPlayerAdapter
-dontwarn com.maxrave.spotify.Spotify
-dontwarn com.maxrave.spotify.model.response.spotify.CanvasResponse$Canvas$ThumbOfCanva
-dontwarn com.maxrave.spotify.model.response.spotify.CanvasResponse$Canvas
-dontwarn com.maxrave.spotify.model.response.spotify.CanvasResponse
-dontwarn com.maxrave.spotify.model.response.spotify.ClientTokenResponse$GrantedToken
-dontwarn com.maxrave.spotify.model.response.spotify.ClientTokenResponse
-dontwarn com.maxrave.spotify.model.response.spotify.PersonalTokenResponse
-dontwarn com.maxrave.spotify.model.response.spotify.SpotifyLyricsResponse$Lyrics$Line
-dontwarn com.maxrave.spotify.model.response.spotify.SpotifyLyricsResponse$Lyrics
-dontwarn com.maxrave.spotify.model.response.spotify.SpotifyLyricsResponse
-dontwarn com.maxrave.spotify.model.response.spotify.search.SpotifySearchResponse$Data$Search$TracksV2$Items$Item$DataX$Duration
-dontwarn com.maxrave.spotify.model.response.spotify.search.SpotifySearchResponse$Data$Search$TracksV2$Items$Item$DataX
-dontwarn com.maxrave.spotify.model.response.spotify.search.SpotifySearchResponse$Data$Search$TracksV2$Items$Item
-dontwarn com.maxrave.spotify.model.response.spotify.search.SpotifySearchResponse$Data$Search$TracksV2$Items
-dontwarn com.maxrave.spotify.model.response.spotify.search.SpotifySearchResponse$Data$Search$TracksV2
-dontwarn com.maxrave.spotify.model.response.spotify.search.SpotifySearchResponse$Data$Search
-dontwarn com.maxrave.spotify.model.response.spotify.search.SpotifySearchResponse$Data
-dontwarn com.maxrave.spotify.model.response.spotify.search.SpotifySearchResponse
-dontwarn org.simpmusic.aiservice.AIHost
-dontwarn org.simpmusic.aiservice.AiClient
-dontwarn org.simpmusic.lyrics.SimpMusicLyricsClient
-dontwarn org.simpmusic.lyrics.domain.Lyrics$LyricsX$Line
-dontwarn org.simpmusic.lyrics.domain.Lyrics$LyricsX
-dontwarn org.simpmusic.lyrics.domain.Lyrics

-keep class org.apache.commons.io.** { *; }

#YtDlp
-keep class com.yausername.** { *; }
-keep class org.apache.commons.compress.archivers.zip.** { *; }
-keepattributes SourceFile