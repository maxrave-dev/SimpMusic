package org.simpmusic.crashlytics

import android.content.Context
import android.util.Log
import com.maxrave.domain.data.player.PlayerError
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid

// Sent crash to Sentry
fun reportCrash(throwable: Throwable) {
    Sentry.captureException(throwable)
}

private const val APP_OPEN = "app.open"

fun configCrashlytics(
    applicationContext: Context,
    dsn: String,
) {
    SentryAndroid.init(applicationContext) { options ->
        Log.d("Sentry", "dsn: $dsn")
        options.dsn = dsn
        options.isSendDefaultPii = true
        options.setTracesSampler { context -> if (context.transactionContext.name == APP_OPEN) 1.0 else 0.0 }
    }
    Sentry.startTransaction(APP_OPEN, APP_OPEN).finish()
}

fun pushPlayerError(error: PlayerError) {
    Sentry.withScope { scope ->
        Sentry.captureMessage("Player Error: ${error.message}, code: ${error.errorCode}, code name: ${error.errorCodeName}")
    }
}