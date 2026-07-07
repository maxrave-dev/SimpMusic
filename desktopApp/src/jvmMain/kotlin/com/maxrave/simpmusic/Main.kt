package com.maxrave.simpmusic

import java.awt.Toolkit

/**
 * Thin entry point for SimpMusic Desktop.
 *
 * All window setup, VLC bootstrap, Sentry init, Koin loading, deep link
 * handling, mini-player wiring, and tray integration live in
 * `composeApp/src/jvmMain/.../main.kt` as `fun runDesktopApp()`. That keeps
 * the shared module self-contained (it can still be launched directly
 * during development) while letting this :desktopApp module own the JVM
 * launcher class Conveyor / compose.desktop point `mainClass` at.
 *
 * The :composeApp KMP library exposes runDesktopApp() publicly so this
 * stub can delegate without duplicating any window-construction logic.
 */
/**
 * Force the X11 WM_CLASS so the Linux taskbar/dock shows "SimpMusic" instead of
 * the class name of whatever thread first touches AWT — with the Conveyor native
 * launcher that ends up being a Kotlin coroutine worker
 * (kotlinx.coroutines...CoroutineScheduler$Worker, hence the wrong dock label).
 *
 * JDK derives WM_CLASS from sun.awt.X11.XToolkit.awtAppClassName, read lazily at
 * window-init time, and there is no supported system property to set it (see the
 * still-open RFE JDK-6528430). So we overwrite the field by reflection AFTER the
 * toolkit is constructed but BEFORE the first window opens. Because we set it
 * post-construction, the value is used verbatim (no '.'->'-' mangling).
 *
 * Requires `--add-opens java.desktop/sun.awt.X11=ALL-UNNAMED` (set for Linux in
 * conveyor.conf). The value MUST match StartupWMClass in the generated .desktop
 * file, else GNOME/KDE won't bind the launcher icon to the window.
 */
private fun forceLinuxWmClass(appName: String = "SimpMusic") {
    if (!System.getProperty("os.name").orEmpty().contains("linux", ignoreCase = true)) return
    runCatching {
        val toolkit = Toolkit.getDefaultToolkit()
        // Only the X11/XWayland toolkit exposes awtAppClassName; skip WLToolkit/headless.
        if (toolkit.javaClass.name != "sun.awt.X11.XToolkit") return
        toolkit.javaClass.getDeclaredField("awtAppClassName").apply {
            isAccessible = true
            set(null, appName)
        }
    }.onFailure { System.err.println("forceLinuxWmClass failed: ${it.message}") }
}

fun main(args: Array<String>) {
    forceLinuxWmClass()
    runDesktopApp(args)
}
