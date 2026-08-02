-optimizations !method/specialization/*,!code/allocation/variable,!method/inlining/*

#########################################################
# 🛡️ SEGURIDAD Y OFUSCACIÓN (SIMPMUSIC) - MODO ESCRITORIO
#########################################################
# ATENCIÓN: -repackageclasses y -flattenpackagehierarchy FUERON ELIMINADOS.
# En Compose Desktop, aplanar los paquetes rompe los enlaces JNI nativos de Skia y cierra la app.

# Modificación de acceso para ofuscar código que de otra forma no se podría
-allowaccessmodification

# Sobrecarga de diccionarios (Mismos nombres para métodos diferentes)
-useuniqueclassmembernames

# Ocultar el código fuente real
-renamesourcefileattribute SimpMusic

# Atributos permitidos
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,*Annotation*,EnclosingMethod

# Destrucción de Metadatos de Kotlin
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullParameter(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
}
#########################################################

# ==========================================
# REGLAS VITALES PARA COMPOSE DESKTOP Y JNA
# ==========================================
# Proteger el punto de entrada de la aplicación
-keep class com.maxrave.simpmusic.MainKt {
    public static void main(java.lang.String[]);
}

# Blindaje absoluto para Compose (Evita que la ventana crashee)
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }
-dontwarn androidx.compose.**
-dontwarn org.jetbrains.compose.**

# Skiko / Skia + Compose AWT interop
-keep class org.jetbrains.skiko.** { *; }
-keep class org.jetbrains.skia.** { *; }
-keep class androidx.compose.ui.awt.** { *; }
-keep class androidx.compose.ui.interop.** { *; }
-dontwarn org.jetbrains.skiko.**
-dontwarn org.jetbrains.skia.**

# JNA (Vital para VLC)
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.Structure {
    public *;
}
-dontwarn com.sun.jna.**

# VLC (vlcj)
-keep class uk.co.caprica.vlcj.** { *; }
-dontwarn uk.co.caprica.vlcj.**
# ==========================================

-keepclasseswithmembers class * {
    native <methods>;
}

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-keep class io.ktor.client.engine.cio.** { *; }
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**
-dontnote io.ktor.**
-dontnote org.slf4j.**
-dontnote kotlinx.serialization.**

# compottie (Lottie renderer)
-keep class io.github.alexzhirkevich.** { *; }
-dontwarn io.github.alexzhirkevich.**

# Okhttp3
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# JavaFX
-keep class javafx.** { *; }
-keep class com.sun.javafx.** { *; }
-dontwarn javafx.**
-dontwarn com.sun.javafx.**

# Probuf
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

-keep class nl.adaptivity.xmlutil.** { *; }
-dontwarn nl.adaptivity.xmlutil.**

-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

-keep class com.maxrave.domain.data.model.** { *; }
-keep class com.mohamedrejeb.ksoup.html.** { *; }
-keep class org.schabi.newpipe.extractor.downloader.** { *; }
-keep class dev.maxrave.pipepipe.extractor.downloader.** { *; }

# Koin y Código Interno de SimpMusic
-keep class org.koin.core.** { *; }
-dontwarn org.koin.**
-keep class com.maxrave.simpmusic.** { *; }

# Default rules
-keep class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlinx.coroutines.internal.MainDispatcherFactory

# kotlinx.coroutines full keep (Swing dispatcher es vital para Desktop)
-keep class kotlinx.coroutines.** { *; }
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlinx.coroutines.flow.internal.ChannelFlow* { <fields>; }
-dontwarn kotlinx.coroutines.**

# androidx.room
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
-keepclassmembers class androidx.room.** { *; }
-dontwarn androidx.room.**

# androidx.sqlite
-keep class androidx.sqlite.** { *; }
-keep interface androidx.sqlite.** { *; }
-dontwarn androidx.sqlite.**

# Serialization
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-dontnote kotlinx.serialization.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn kotlinx.serialization.internal.ClassValueReferences
-keep class com.maxrave.simpmusic.data.model.** { *; }
-keep class com.maxrave.simpmusic.extension.AllExtKt { *; }
-keep class com.maxrave.simpmusic.extension.AllExtKt$* { *; }
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

# Retrofit
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

-keep,allowobfuscation,allowshrinking class retrofit2.Response

# OkHttp
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

## More rules for Extractors
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class dev.maxrave.pipepipe.extractor.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter

-dontwarn com.maxrave.data.di.loader.LoaderKt
-dontwarn com.maxrave.media3.ui.MediaPlayerViewKt

-keep class com.maxrave.data.di.loader.LoaderKt { *; }
-keep class com.maxrave.data.mapping.MappingKt { *; }
-keep class com.maxrave.data.extension.** { *; }
-keep class com.maxrave.data.di.** { *; }

-keep class org.simpmusic.lyrics.parser.** { *; }
-keep class org.simpmusic.lyrics.models.** { *; }
-keep class org.simpmusic.nowplayingcenter.** { *; }
-keep class io.github.selemba1000.** { *; }
-keep class com.simpmusic.lyrics.parser.** { *; }

# dbus-java
-keep class org.freedesktop.dbus.** { *; }
-keep class com.github.hypfvieh.** { *; }
-dontwarn org.freedesktop.dbus.**
-dontwarn com.github.hypfvieh.**
-keepnames class org.freedesktop.dbus.spi.transport.ITransportProvider
-keep class * implements org.freedesktop.dbus.spi.transport.ITransportProvider { *; }
-adaptresourcefilecontents META-INF/services/**
-keepnames class * implements java.util.ServiceLoader$Provider

-keep class com.google.re2j.** { *; }
-dontwarn com.google.re2j.**

# Wire
-dontwarn android.os.**
-keep class com.squareup.wire.** { *; }
-keep interface com.squareup.wire.** { *; }
-dontwarn com.squareup.wire.**
-keep class com.grack.nanojson.** { *; }
-dontwarn com.grack.nanojson.**

# org.json (JSON-Java)
-keep class org.json.** { *; }

# Brave bundles BitChute
-keep class com.github.bravenewpipe.** { *; }
-dontwarn com.github.bravenewpipe.**

# PipePipe Rhino
-dontwarn org.mozilla.javascript.ObjToIntMap

-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.datastore.preferences.** { *; }

# cache2k
-dontwarn kotlin.annotations.jvm.**
-dontwarn org.cache2k.**

# Haze & Liquid glass
-dontwarn dev.chrisbanes.haze.**
-dontwarn com.kyant.backdrop.**

# JNA (Redundante pero seguro)
-dontwarn com.sun.jna.**
-dontnote **
-dontoptimize

# Mantener las librerías de red (OkHttp y Ktor) a salvo de la eliminación
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
-dontwarn io.ktor.**

# Mantener las librerías de Coil (Imágenes) a salvo
-keep class coil3.** { *; }
-keep interface coil3.** { *; }
-keep class coil3.network.okhttp.** { *; }
-dontwarn coil3.**

# Mantener el extractor de colores dinámicos (KMPalette) a salvo
-keep class com.kmpalette.** { *; }
-keep interface com.kmpalette.** { *; }
-dontwarn com.kmpalette.**

# Evitar que ProGuard rompa la des-serialización JSON (si la usas para red)
-keep class kotlinx.serialization.** { *; }
-keep interface kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Mantener los reflectores nativos de Java
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod