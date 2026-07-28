# =========================================================
# 🛡️ NIVEL MÁXIMO DE OFUSCACIÓN (EXTREMA)
# =========================================================

# 1. Destruir la jerarquía de paquetes (mueve todas las clases a la raíz)
-repackageclasses ''
-flattenpackagehierarchy ''

# 2. Permitir modificación de accesos para forzar ofuscación profunda e inlining
-allowaccessmodification

# 3. Eliminar metadatos, tablas de variables, nombres originales y números de línea
-renamesourcefileattribute ''
-keepattributes !LineNumberTable, !LocalVariableTable, !LocalVariableTypeTable, !SourceFile, !SourceDir

# 4. Optimizaciones agresivas
-mergeinterfacesaggressively
-overloadaggressively

# 5. Eliminar todos los logs y prints de consola silenciosamente
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# =========================================================
# 📚 REGLAS DE LIBRERÍAS (NECESARIAS PARA QUE NO FALLE LA APP)
# =========================================================

-keep class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlinx.coroutines.internal.MainDispatcherFactory

# Keep `Companion` object fields of serializable classes.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects of serializable classes.
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

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-dontnote kotlinx.serialization.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn kotlinx.serialization.internal.ClassValueReferences

# --- RUTAS DE PAQUETE ACTUALIZADAS A com.ytmusic.kurompx ---
-keep class com.ytmusic.kurompx.data.model.** { *; }
-keep class com.ytmusic.kurompx.extension.AllExtKt { *; }
-keep class com.ytmusic.kurompx.extension.AllExtKt$* { *; }
-keep class com.maxrave.kotlinytmusicscraper.extension.MapExtKt$* { *; }

## Rules for NewPipeExtractor
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class dev.maxrave.pipepipe.extractor.timeago.patterns.** { *; }
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.tools.**

-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor

# Retrofit (CORREGIDO PARA EVITAR 'API FAILED')
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn retrofit2.**

-keepclassmembers,allowshrinking interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep interface <1>
-if interface * { @retrofit2.http.* <methods>; }
-keep interface * extends <1>
-keep,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking class <3>

-keep,allowshrinking class retrofit2.Response

# Proteger modelos de datos JSON para que no falle la API al desofuscar
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Animal Sniffer y OkHttp
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.internal.Util

-keep class com.liskovsoft.** { *; }
-keep interface com.liskovsoft.** { *; }
-keep class com.eclipsesource.v8.** { *; }
-keep class com.maxrave.kotlinytmusicscraper.** { *; }

-dontwarn javax.script.**
-dontwarn jdk.dynalink.**

-keep class org.apache.commons.io.** { *; }

# YtDlp
-keep class com.yausername.** { *; }
-keep class org.apache.commons.compress.archivers.zip.** { *; }

## Rules for NewPipeExtractor (ClassFile)
-dontwarn org.mozilla.classfile.**
-keep class org.mozilla.classfile.ClassFileWriter

-dontwarn com.maxrave.data.di.loader.LoaderKt
-dontwarn com.maxrave.media3.ui.MediaPlayerViewKt

-keep class com.maxrave.data.di.loader.LoaderKt { *; }
-keep class com.maxrave.data.mapping.MappingKt { *; }
-keep class com.maxrave.data.extension.** { *; }
-keep class com.maxrave.data.di.** { *; }

-keep class com.maxrave.kotlinytmusicscraper.** { *; }

-keep class org.simpmusic.lyrics.parser.** { *; }
-keep class org.simpmusic.lyrics.models.** { *; }
-keep class com.simpmusic.lyrics.parser.** { *; }

-keep class com.google.re2j.** { *; }
-dontwarn com.google.re2j.Matcher
-dontwarn com.google.re2j.Pattern

-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase { <init>(); }

-dontwarn io.sentry.**

# Solucionar error de clases faltantes de Window/Sidecar en Release
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**
-dontwarn androidx.window.layout.adapter.sidecar.**