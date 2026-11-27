package com.maxrave.simpmusic.expect

/** The launcher icons a user can pick in Settings. LIQUID_GLASS is the icon the app ships with. */
enum class AppIconStyle { LIQUID_GLASS, OLD_SCHOOL, MINIMALISM_LIGHT, MINIMALISM_DARK, PIXEL }

/** The launcher icon in use, or null where the app cannot change it (Desktop), which hides the setting. */
expect fun currentAppIcon(): AppIconStyle?

/** Switches the launcher icon, then closes the app: the user reopens it from the new icon. */
expect fun setAppIcon(style: AppIconStyle)
