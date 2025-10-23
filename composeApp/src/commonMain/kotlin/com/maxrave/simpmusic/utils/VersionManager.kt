package com.maxrave.simpmusic.utils

import com.maxrave.simpmusic.BuildKonfig
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.getPlatform

object VersionManager {
    private var versionName: String? = null

    fun initialize() {
        if (versionName == null) {
            versionName =
                try {
                    BuildKonfig.versionName
                } catch (_: Exception) {
                    String()
                }
        }
    }

    fun getVersionName(): String = removeDevSuffix(versionName ?: String())

    private fun removeDevSuffix(versionName: String): String {
        val ver =
            if (versionName.endsWith("-dev")) {
                versionName.replace("-dev", "")
            } else {
                versionName
            }
        if (getPlatform() == Platform.Desktop) {
            return "$ver-alpha02-(${getPlatform().osName()})"
        } else {
            return ver
        }
    }
}