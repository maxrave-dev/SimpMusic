package com.maxrave.simpmusic.ui.component

import androidx.compose.ui.awt.ComposeWindow
import com.maxrave.logger.Logger
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.awt.Window

/**
 * Minimizes a window with `-[NSWindow miniaturize:]`, the call the yellow button of any Mac app makes.
 *
 * Going through AWT (`extendedState = ICONIFIED`, which is what Compose's `WindowState.isMinimized`
 * does) is not equivalent: for a zoomed window the JDK first un-zooms it and only then miniaturizes,
 * so the window visibly shrinks to its small size and the Dock animation starts from that. Measured:
 * 1470x860 -> 1249x731 about 0.3 s before the iconify event. Native miniaturize leaves the frame alone,
 * so a zoomed window goes into the Dock zoomed and comes back zoomed.
 */
internal object MacMiniaturize {
    private const val TAG = "MacMiniaturize"

    private class Natives {
        private val objc = NativeLibrary.getInstance("objc")
        private val system = NativeLibrary.getInstance("System")
        val msgSend = objc.getFunction("objc_msgSend")
        val dispatchAsync = system.getFunction("dispatch_async_f")
        val mainQueue: Pointer = system.getGlobalVariableAddress("_dispatch_main_q")
        val miniaturize: Pointer = objc.getFunction("sel_registerName").invokePointer(arrayOf("miniaturize:"))
    }

    private val natives: Natives? by lazy {
        if (!System.getProperty("os.name", "").contains("Mac", ignoreCase = true)) return@lazy null
        runCatching { Natives() }
            .onFailure { Logger.w(TAG, "Native miniaturize unavailable, minimizing through AWT: $it") }
            .getOrNull()
    }

    // One instance for the whole process: JNA frees a callback's native stub once its object is
    // garbage collected, and the queued call runs after minimize() has returned.
    private val work =
        object : MacMainQueueWork {
            override fun invoke(context: Pointer?) {
                val n = natives ?: return
                runCatching { n.msgSend.invokeVoid(arrayOf(context, n.miniaturize, null)) }
                    .onFailure { Logger.w(TAG, "miniaturize: failed: $it") }
            }
        }

    /** True when the call was queued; false off macOS or when it cannot be made (the caller falls back). */
    fun minimize(window: Window): Boolean {
        val n = natives ?: return false
        val handle = (window as? ComposeWindow)?.windowHandle ?: return false
        if (handle == 0L) return false
        return runCatching {
            n.dispatchAsync.invokeVoid(arrayOf(n.mainQueue, Pointer(handle), work))
        }.isSuccess
    }
}
