package com.maxrave.simpmusic.ui.component

import com.maxrave.logger.Logger
import java.awt.Frame
import java.awt.Rectangle

/**
 * Remembers where and how big the user left the main window, and puts it back after a minimise or a
 * hide to the tray. AWT and Compose only track the size; nothing holds it, and an undecorated window
 * on macOS can come back from the Dock at a different frame (or lose its zoomed state), which read
 * as "it reset itself to the default size".
 */
internal class WindowArrangement {
    private var bounds: Rectangle? = null
    private var maximized = false

    private var lastResizeLogAt = 0L

    /** One log line per window event, so a reset can be read back from app.log. */
    fun log(event: String, window: Frame, throttle: Boolean = false) {
        val now = System.currentTimeMillis()
        if (throttle) {
            if (now - lastResizeLogAt < 400) return
            lastResizeLogAt = now
        }
        Logger.i(
            TAG,
            "$event bounds=${window.bounds} ext=${window.extendedState} showing=${window.isShowing} " +
                "noted=$bounds notedMax=$maximized",
        )
    }

    /** Records the current frame. Ignored while hidden or minimized, when the numbers mean nothing. */
    fun note(window: Frame) {
        if (!window.isShowing) return
        if (window.extendedState and Frame.ICONIFIED != 0) return
        if (window.width <= 0 || window.height <= 0) return
        bounds = window.bounds
        maximized = window.extendedState and Frame.MAXIMIZED_BOTH == Frame.MAXIMIZED_BOTH
    }

    /**
     * Puts the window back as last noted, only when something moved it. A zoomed window is left
     * alone: re-zooming after the restore made it visibly jump, and the zoom is no longer lost
     * (see [MacMiniaturize]).
     */
    fun putBack(window: Frame) {
        val saved = bounds ?: return
        if (maximized) return
        if (window.bounds != saved) {
            Logger.i(TAG, "Window came back as ${window.bounds}, restoring $saved")
            window.bounds = saved
        }
    }

    private companion object {
        const val TAG = "WindowArrangement"
    }
}
