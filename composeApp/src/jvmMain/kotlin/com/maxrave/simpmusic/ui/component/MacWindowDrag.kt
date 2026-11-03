package com.maxrave.simpmusic.ui.component

import androidx.compose.ui.awt.ComposeWindow
import com.maxrave.logger.Logger
import com.sun.jna.Callback
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.awt.Window

/**
 * Hands a window drag to macOS with `-[NSWindow performWindowDragWithEvent:]`, so the Window Server
 * moves the window the way it moves every other Mac app: drawn on both displays while it crosses
 * between them, and taking part in Spaces and Mission Control. A window moved from code with
 * `setLocation` stays drawn on one display and then jumps whole to the other.
 */
internal object MacWindowDrag {
    private const val TAG = "MacWindowDrag"
    private const val LEFT_MOUSE_DOWN = 1L // NSEventTypeLeftMouseDown
    private const val LEFT_MOUSE_DRAGGED = 6L // NSEventTypeLeftMouseDragged

    private class Natives {
        private val objc = NativeLibrary.getInstance("objc")
        private val system = NativeLibrary.getInstance("System")
        private val registerSelector = objc.getFunction("sel_registerName")
        val msgSend = objc.getFunction("objc_msgSend")
        val dispatchAsync = system.getFunction("dispatch_async_f")

        // dispatch_get_main_queue() is &_dispatch_main_q, so the symbol's address is the queue.
        val mainQueue: Pointer = system.getGlobalVariableAddress("_dispatch_main_q")
        val appClass: Pointer = objc.getFunction("objc_getClass").invokePointer(arrayOf("NSApplication"))
        val sharedApplication = sel("sharedApplication")
        val currentEvent = sel("currentEvent")
        val type = sel("type")
        val performWindowDrag = sel("performWindowDragWithEvent:")

        private fun sel(name: String): Pointer = registerSelector.invokePointer(arrayOf(name))
    }

    private val natives: Natives? by lazy {
        if (!System.getProperty("os.name", "").contains("Mac", ignoreCase = true)) return@lazy null
        runCatching { Natives() }
            .onFailure { Logger.w(TAG, "Native window drag unavailable, windows move from code: $it") }
            .getOrNull()
    }

    // Runs on the AppKit main thread, with the NSWindow as context. One instance for the whole
    // process: JNA frees a callback's native stub once its object is garbage collected.
    private val work =
        object : MacMainQueueWork {
            override fun invoke(context: Pointer?) {
                val n = natives ?: return
                runCatching {
                    val app = n.msgSend.invokePointer(arrayOf(n.appClass, n.sharedApplication))
                    val event = n.msgSend.invokePointer(arrayOf(app, n.currentEvent)) ?: return
                    // The button may be up by the time this runs; only a held left button starts a drag.
                    val type = n.msgSend.invokeLong(arrayOf(event, n.type))
                    if (type == LEFT_MOUSE_DOWN || type == LEFT_MOUSE_DRAGGED) {
                        n.msgSend.invokeVoid(arrayOf(context, n.performWindowDrag, event))
                    }
                }.onFailure { Logger.w(TAG, "performWindowDragWithEvent failed: $it") }
            }
        }

    /**
     * Starts a native drag of [window] for the drag gesture in progress. Returns false off macOS or
     * when the call cannot be made, and the caller then moves the window itself.
     *
     * Apple's docs warn that a mouse-up may not be sent after this; Compose's SyntheticEventSender
     * synthesizes the missing release from the next mouse event, so the gesture still ends.
     */
    fun start(window: Window): Boolean {
        val n = natives ?: return false
        val handle = (window as? ComposeWindow)?.windowHandle ?: return false
        if (handle == 0L) return false
        // Async on purpose: AppKit can be waiting on the AWT thread while it delivers mouse events,
        // so waiting on AppKit from here could leave the two threads waiting on each other.
        return runCatching {
            n.dispatchAsync.invokeVoid(arrayOf(n.mainQueue, Pointer(handle), work))
        }.isSuccess
    }
}

/** `dispatch_function_t`. Top-level, like core/data's `DispatchWork`, so JNA can reach its method. */
internal interface MacMainQueueWork : Callback {
    fun invoke(context: Pointer?)
}
