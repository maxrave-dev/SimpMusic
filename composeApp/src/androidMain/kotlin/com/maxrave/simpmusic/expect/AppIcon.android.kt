package com.maxrave.simpmusic.expect

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.maxrave.media3.di.stopService
import org.koin.mp.KoinPlatform.getKoin

// One <activity-alias> per icon in androidApp's manifest. The class names are fixed forever: renaming one
// strands every install that has it enabled.
private fun AppIconStyle.aliasName(): String =
    when (this) {
        AppIconStyle.LIQUID_GLASS -> "com.maxrave.simpmusic.AppIconLiquidGlass"
        AppIconStyle.OLD_SCHOOL -> "com.maxrave.simpmusic.AppIconOldSchool"
        AppIconStyle.MINIMALISM_LIGHT -> "com.maxrave.simpmusic.AppIconMinimalismLight"
        AppIconStyle.MINIMALISM_DARK -> "com.maxrave.simpmusic.AppIconMinimalismDark"
        AppIconStyle.PIXEL -> "com.maxrave.simpmusic.AppIconPixel"
    }

actual fun currentAppIcon(): AppIconStyle? {
    val context: Context = getKoin().get()
    val packageManager = context.packageManager
    // An alias never switched is in its manifest state: only LIQUID_GLASS is enabled there.
    return AppIconStyle.entries.firstOrNull { style ->
        packageManager.getComponentEnabledSetting(ComponentName(context, style.aliasName())) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } ?: AppIconStyle.LIQUID_GLASS
}

actual fun setAppIcon(style: AppIconStyle) {
    val context: Context = getKoin().get()
    val packageManager = context.packageManager
    // Enable the new one first, so the app is never left with no launcher entry.
    packageManager.setComponentEnabledSetting(
        ComponentName(context, style.aliasName()),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP,
    )
    AppIconStyle.entries.filter { it != style }.forEach {
        packageManager.setComponentEnabledSetting(
            ComponentName(context, it.aliasName()),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
    // DONT_KILL_APP keeps the process alive until every alias is switched; closing it is the user's
    // confirmed choice, done the way a backup restore does it (stop playback, then exit).
    stopService(context)
    Runtime.getRuntime().exit(0)
}
