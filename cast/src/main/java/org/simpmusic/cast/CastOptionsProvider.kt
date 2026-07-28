package org.simpmusic.cast

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions

// Declared as the OPTIONS_PROVIDER_CLASS_NAME meta-data target in
// AndroidManifest.xml. Instantiated by the Cast framework via reflection —
// see consumer-rules.pro for the matching R8 keep rule.
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val mediaOptions =
            CastMediaOptions
                .Builder()
                // The app's own Media3 MediaSession is the single source of truth for
                // playback controls and the notification — do not let the Cast
                // framework create a competing session or notification of its own.
                .setMediaSessionEnabled(false)
                .setNotificationOptions(null)
                .build()

        return CastOptions
            .Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
