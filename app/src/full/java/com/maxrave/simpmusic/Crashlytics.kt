package com.maxrave.simpmusic

import android.content.Context
import android.util.Log
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid

// Sent crash to Sentry
fun reportCrash(throwable: Throwable) {
    Sentry.captureException(throwable)
}

fun configCrashlytics(applicationContext: Context) {
    SentryAndroid.init(applicationContext) { options ->
        val dsn = BuildConfig.SENTRY_DSN
        Log.d("Sentry", "dsn: $dsn")
        options.dsn = dsn
    }
}

fun pushYouTubeError(error: Throwable) {
    Sentry.withScope { scope ->
        Sentry.captureException(error)
    }
}