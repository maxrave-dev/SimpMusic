package com.maxrave.simpmusic.ui.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.maxrave.logger.Logger
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.awt.Rectangle
import java.awt.Window

/**
 * Makes the custom-title-bar window cover its whole screen, the way the green button does in other
 * Mac apps. macOS native full screen (`toggleFullScreen:`, what `WindowPlacement.Fullscreen` calls)
 * hides the menu bar but does not resize a borderless window, which stayed at its old size on a
 * black Space. So the window is sized to the screen from code instead, and the menu bar and Dock
 * are hidden with `NSApplication.presentationOptions`.
 */
internal object MacFillScreen {
    private const val TAG = "MacFillScreen"

    // NSApplicationPresentationAutoHideDock | NSApplicationPresentationAutoHideMenuBar. AppKit
    // rejects hiding the menu bar unless the Dock is hidden too.
    private const val HIDE_DOCK_AND_MENU_BAR = (1L shl 0) or (1L shl 2)

    private var restoreBounds: Rectangle? = null

    /** Compose state, so the window shell can square its corners while the screen is filled. */
    var isFilled by mutableStateOf(false)
        private set

    fun toggle(window: Window) {
        val saved = restoreBounds
        if (saved == null) {
            restoreBounds = window.bounds
            isFilled = true
            window.bounds = window.graphicsConfiguration.bounds
            setPresentationOptions(HIDE_DOCK_AND_MENU_BAR)
        } else {
            restoreBounds = null
            isFilled = false
            setPresentationOptions(0L)
            window.bounds = saved
        }
    }

    /**
     * Gives the menu bar and Dock back while the window is minimized or hidden, WITHOUT leaving the
     * fill: the window keeps its size, so it comes back as it was.
     */
    fun giveBackScreenEdges() {
        if (isFilled) setPresentationOptions(0L)
    }

    /** Hides the menu bar and Dock again when a filled window comes back. */
    fun takeScreenEdges() {
        if (isFilled) setPresentationOptions(HIDE_DOCK_AND_MENU_BAR)
    }

    /** Called when the window is disposed while filled, so the menu bar does not stay hidden. */
    fun release(window: Window) {
        if (restoreBounds != null) toggle(window)
    }

    private fun setPresentationOptions(options: Long) {
        runCatching {
            val objc = NativeLibrary.getInstance("objc")
            val system = NativeLibrary.getInstance("System")
            val msgSend = objc.getFunction("objc_msgSend")
            val sel = objc.getFunction("sel_registerName")
            val appClass = objc.getFunction("objc_getClass").invokePointer(arrayOf("NSApplication"))
            val shared = sel.invokePointer(arrayOf("sharedApplication"))
            val setOptions = sel.invokePointer(arrayOf("setPresentationOptions:"))
            val app = msgSend.invokePointer(arrayOf(appClass, shared))
            // AppKit state belongs on the main thread, not the AWT one.
            val work =
                object : MacMainQueueWork {
                    override fun invoke(context: Pointer?) {
                        runCatching { msgSend.invokeVoid(arrayOf(app, setOptions, options)) }
                            .onFailure { Logger.w(TAG, "setPresentationOptions failed: $it") }
                    }
                }
            keepAlive = work
            system.getFunction("dispatch_async_f").invokeVoid(
                arrayOf(system.getGlobalVariableAddress("_dispatch_main_q"), null, work),
            )
        }.onFailure { Logger.w(TAG, "Could not change presentation options: $it") }
    }

    // JNA frees a callback's native stub once its object is garbage collected, and the queued call
    // runs after this function returns.
    @Volatile private var keepAlive: MacMainQueueWork? = null
}
