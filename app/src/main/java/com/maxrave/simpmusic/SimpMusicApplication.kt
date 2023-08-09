package com.maxrave.simpmusic

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp
import org.acra.config.MailSenderConfiguration
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

@HiltAndroidApp
class SimpMusicApplication: Application(){
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        initAcra {
            //core configuration:
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            //each plugin you chose above can be configured in a block like this:
            mailSender {
                //required
                mailTo = "simpmusic.maxravedev@gmail.com"
                //defaults to true
                reportAsFile = true
                //defaults to ACRA-report.stacktrace
                reportFileName = "Crash.txt"
                //defaults to "<applicationId> Crash Report"
                subject = getString(R.string.mail_subject)
                //defaults to empty
            }
        }
    }
}