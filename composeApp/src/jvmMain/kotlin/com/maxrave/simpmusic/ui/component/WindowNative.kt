package com.maxrave.simpmusic.ui.component

import java.awt.Window

/**
 * Native window management helpers (Windows-only today).
 *
 * An undecorated Compose window has no OS frame, so AWT has no non-client zone to hand the
 * move/resize loop to — Compose's own UndecoratedWindowResizer fakes it with 6px edge strips that
 * call setBounds(). Windows exposes a much better path anyway: send WM_NCLBUTTONDOWN with a
 * non-client hit-test code (HTCAPTION to move, HTTOP/HTLEFT/... to resize) and the OS runs its
 * native modal loop — smooth, with Aero snap, edge snapping and native cursors, exactly like a
 * decorated window. This is the trick IntelliJ-platform and Electron-style apps use.
 */
object WindowNative {
    private const val WM_NCLBUTTONDOWN = 0x00A1

    // WM_NCHITTEST codes we hand to the OS.
    const val HT_CAPTION = 2
    const val HT_LEFT = 10
    const val HT_RIGHT = 11
    const val HT_TOP = 12
    const val HT_TOPLEFT = 13
    const val HT_TOPRIGHT = 14
    const val HT_BOTTOM = 15
    const val HT_BOTTOMLEFT = 16
    const val HT_BOTTOMRIGHT = 17

    val isWindows: Boolean by lazy {
        System.getProperty("os.name", "").contains("Win", ignoreCase = true)
    }

    /**
     * Start the OS-native move/resize loop for this window. No-op off Windows.
     * [hitTest] is one of the [HT_] constants.
     */
    fun startNativeWindowCommand(window: Window, hitTest: Int) {
        if (!isWindows) return
        val hwnd = nativeHandle(window) ?: return
        try {
            val user32 = com.sun.jna.platform.win32.User32.INSTANCE
            // ReleaseCapture first: if AWT grabbed the capture, Windows refuses to enter the
            // move/resize loop and the drag just dies on the first pixel.
            // (jna-platform's User32 interface does not declare it, so bind it separately.)
            User32Ext.INSTANCE.ReleaseCapture()
            user32.SendMessage(
                com.sun.jna.platform.win32.WinDef.HWND(com.sun.jna.Pointer(hwnd)),
                WM_NCLBUTTONDOWN,
                com.sun.jna.platform.win32.WinDef.WPARAM(hitTest.toLong()),
                com.sun.jna.platform.win32.WinDef.LPARAM(0),
            )
        } catch (e: Throwable) {
            // Not a real Windows peer or the handle is stale — leave the call a no-op.
        }
    }

    /** Minimal user32 binding for the few functions missing from jna-platform's User32. */
    private interface User32Ext : com.sun.jna.win32.StdCallLibrary {
        fun ReleaseCapture(): Boolean

        companion object {
            val INSTANCE: User32Ext =
                com.sun.jna.Native.load("user32", User32Ext::class.java)
        }
    }

    /**
     * Resolve the native HWND for an AWT [Window] by reflecting `java.awt.Component.peer` and
     * calling `getHWnd()` on the Windows peer. Requires
     * `--add-opens=java.desktop/java.awt=ALL-UNNAMED` (added for the desktop app in
     * desktopApp/build.gradle.kts).
     */
    private fun nativeHandle(window: Window): Long? {
        return try {
            val peerField = java.awt.Component::class.java.getDeclaredField("peer")
            peerField.isAccessible = true
            val peer = peerField.get(window) ?: return null
            val method = peer.javaClass.getMethod("getHWnd")
            method.invoke(peer) as Long
        } catch (e: Throwable) {
            null
        }
    }
}
