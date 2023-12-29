package com.maxrave.simpmusic

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.media3.common.util.UnstableApi
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.maxrave.simpmusic.ui.MainActivity
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class SimpMusicApplication: Application(){
    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        CaocConfig.Builder.create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) //default: CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM
            .enabled(true) //default: true
            .showErrorDetails(true) //default: true
            .showRestartButton(true) //default: true
            .errorDrawable(R.mipmap.ic_launcher_round)
            .logErrorOnRestart(false) //default: true
            .trackActivities(true) //default: false
            .minTimeBetweenCrashesMs(2000) //default: 3000 //default: bug image
            .restartActivity(MainActivity::class.java) //default: null (your app's launch activity)
            .apply()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w("Low Memory", "Checking")
    }

    override fun onTerminate() {
        super.onTerminate()

        Log.w("Terminate", "Checking")
    }
}