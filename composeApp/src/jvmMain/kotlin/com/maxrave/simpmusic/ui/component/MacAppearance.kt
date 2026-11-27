package com.maxrave.simpmusic.ui.component

import com.maxrave.logger.Logger
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer

/**
 * Sets the app's NSAppearance so the system title bar, its title and AppKit's own dialogs follow the
 * app's theme. AWT forces light Aqua at launch unless `apple.awt.application.appearance` is set
 * ("dark mode is not supported yet", NSApplicationAWT.m in JDK 21), and that property is read only
 * once, so a theme picked in Settings needs this call to reach the title bar.
 */
internal object MacAppearance {
    private const val TAG = "MacAppearance"

    private class Natives {
        private val objc = NativeLibrary.getInstance("objc")
        private val system = NativeLibrary.getInstance("System")
        private val registerSelector = objc.getFunction("sel_registerName")
        private val getClass = objc.getFunction("objc_getClass")
        val msgSend = objc.getFunction("objc_msgSend")
        val dispatchAsync = system.getFunction("dispatch_async_f")
        val mainQueue: Pointer = system.getGlobalVariableAddress("_dispatch_main_q")
        val appClass: Pointer = getClass.invokePointer(arrayOf("NSApplication"))
        val appearanceClass: Pointer = getClass.invokePointer(arrayOf("NSAppearance"))
        val stringClass: Pointer = getClass.invokePointer(arrayOf("NSString"))
        val sharedApplication = sel("sharedApplication")
        val appearanceNamed = sel("appearanceNamed:")
        val stringWithUTF8String = sel("stringWithUTF8String:")
        val setAppearance = sel("setAppearance:")

        private fun sel(name: String): Pointer = registerSelector.invokePointer(arrayOf(name))
    }

    private val natives: Natives? by lazy {
        if (!System.getProperty("os.name", "").contains("Mac", ignoreCase = true)) return@lazy null
        runCatching { Natives() }
            .onFailure { Logger.w(TAG, "Title bar keeps the light appearance: $it") }
            .getOrNull()
    }

    @Volatile private var dark = false

    // One instance for the whole process: JNA frees a callback's native stub once it is collected.
    private val work =
        object : MacMainQueueWork {
            override fun invoke(context: Pointer?) {
                val n = natives ?: return
                runCatching {
                    val name = if (dark) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua"
                    val nsName = n.msgSend.invokePointer(arrayOf(n.stringClass, n.stringWithUTF8String, name))
                    val appearance = n.msgSend.invokePointer(arrayOf(n.appearanceClass, n.appearanceNamed, nsName))
                    val app = n.msgSend.invokePointer(arrayOf(n.appClass, n.sharedApplication))
                    n.msgSend.invokeVoid(arrayOf(app, n.setAppearance, appearance))
                }.onFailure { Logger.w(TAG, "setAppearance failed: $it") }
            }
        }

    /** No-op off macOS. */
    fun set(dark: Boolean) {
        val n = natives ?: return
        this.dark = dark
        // Async, as in MacWindowDrag: AppKit may be waiting on the AWT thread this runs on.
        runCatching { n.dispatchAsync.invokeVoid(arrayOf(n.mainQueue, null, work)) }
            .onFailure { Logger.w(TAG, "dispatch_async_f failed: $it") }
    }
}
