package com.maxrave.simpmusic

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
fun main(args: Array<String>) {
    runDesktopApp(args)
}
