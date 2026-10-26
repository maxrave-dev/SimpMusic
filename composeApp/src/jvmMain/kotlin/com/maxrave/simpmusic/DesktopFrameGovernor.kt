package com.maxrave.simpmusic

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.awaitApplication
import com.maxrave.domain.data.player.AppOnScreen
import com.maxrave.logger.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.awt.AWTEvent
import java.awt.Desktop
import java.awt.EventQueue
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.Window
import java.awt.desktop.AppHiddenEvent
import java.awt.desktop.AppHiddenListener
import java.awt.event.ComponentEvent
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.nanoseconds

/**
 * Decides how often SimpMusic's Desktop windows may draw, for every window at once.
 *
 * Vsync caps every frame at the display's refresh rate (it is on everywhere, see runDesktopApp). What
 * it cannot do is stop drawing out of sight: skiko only stops drawing a window that is hidden, and a
 * minimized window still counts as shown — so a minimized window's content is hidden here (see
 * [update]), which stops its drawing outright. And it caps nothing below the refresh rate, which is
 * what this policy adds for animations that never end.
 *
 * Compose asks the [InfiniteAnimationPolicy] in the coroutine context before every frame of an
 * animation that never ends, and every window inherits its context from [governedApplication]:
 *  - a SimpMusic window has focus → as fast as vsync lets them;
 *  - windows on screen, none focused → [UNFOCUSED_FPS];
 *  - every window minimized or hidden, or the app hidden (⌘H) → those animations wait, and
 *    [AppOnScreen] tells the player to drop its position ticks to one a second and stop decoding video.
 *
 * Animations that end by themselves (a ripple, a page transition) are left alone. A loop that
 * redraws every frame for as long as something plays must take its frames from
 * `withInfiniteAnimationFrameNanos`, not `withFrameNanos`, or it slips past this.
 */
object DesktopFrameGovernor : InfiniteAnimationPolicy {
    private const val TAG = "FrameGovernor"
    private const val UNFOCUSED_FPS = 30
    private const val NANOS_PER_SECOND = 1_000_000_000L

    private sealed interface Mode {
        /** [frameIntervalNanos] 0 means no pacing of our own: vsync caps it. */
        data class OnScreen(
            val frameIntervalNanos: Long,
        ) : Mode

        data object OffScreen : Mode
    }

    // Starts on screen and unfocused until the first window event says otherwise: a missed event then
    // costs 30 fps, where starting off screen would freeze every animation in the app.
    private val mode = MutableStateFlow<Mode>(Mode.OnScreen(NANOS_PER_SECOND / UNFOCUSED_FPS))

    @Volatile
    private var appHidden = false

    override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R {
        val interval = mode.filterIsInstance<Mode.OnScreen>().first().frameIntervalNanos
        if (interval > 0) {
            // Wake on a grid every animation shares, so they all land in one frame instead of each
            // asking for its own.
            delay((interval - System.nanoTime() % interval).nanoseconds)
        }
        return block()
    }

    /** Must run before the first window opens: it learns about windows from their AWT events. */
    fun install() {
        Toolkit.getDefaultToolkit().addAWTEventListener(
            { event ->
                // Moves stay in: a window dragged to another monitor may need another refresh rate.
                if (event.source is Window && event.id != ComponentEvent.COMPONENT_RESIZED) {
                    EventQueue.invokeLater(::update)
                }
            },
            AWTEvent.WINDOW_EVENT_MASK or
                AWTEvent.WINDOW_STATE_EVENT_MASK or
                AWTEvent.WINDOW_FOCUS_EVENT_MASK or
                AWTEvent.COMPONENT_EVENT_MASK,
        )
        // ⌘H hides the whole app without hiding or minimizing a single window.
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_EVENT_HIDDEN)) {
            Desktop.getDesktop().addAppEventListener(
                object : AppHiddenListener {
                    override fun appHidden(e: AppHiddenEvent) {
                        appHidden = true
                        update()
                    }

                    override fun appUnhidden(e: AppHiddenEvent) {
                        appHidden = false
                        update()
                    }
                },
            )
        }
    }

    private fun update() {
        val anyOnScreen =
            Window.getWindows().any { window ->
                window.isShowing && !(window is Frame && (window.extendedState and Frame.ICONIFIED) != 0)
            }
        val active = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
        val next =
            when {
                appHidden || !anyOnScreen -> Mode.OffScreen
                active != null -> Mode.OnScreen(0L)
                else -> Mode.OnScreen(NANOS_PER_SECOND / UNFOCUSED_FPS)
            }
        if (next != mode.value) Logger.d(TAG, "Frames: $next")
        mode.value = next
        AppOnScreen.set(next != Mode.OffScreen)

        // Skiko stops drawing a window only once its layer is not showing, and AWT counts a minimized
        // frame as showing, so a minimized SimpMusic went on drawing at full speed (~300 fps measured
        // on Linux). Hiding the content while minimized makes skiko skip every frame, whoever asks.
        for (window in Window.getWindows()) {
            if (window !is ComposeWindow) continue
            val minimized = (window.extendedState and Frame.ICONIFIED) != 0
            if (window.contentPane.isVisible == minimized) {
                Logger.d(TAG, "${window.title}: ${if (minimized) "minimized, drawing stopped" else "restored, drawing again"}")
                window.contentPane.isVisible = !minimized
            }
        }
    }
}

/** `application`, with every window it opens paced by [DesktopFrameGovernor]. */
fun governedApplication(content: @Composable ApplicationScope.() -> Unit) {
    DesktopFrameGovernor.install()
    // `Window` builds its ComposeWindow from the coroutine context of the composition around it, which
    // is this one: the policy reaches every window's animations without touching the windows.
    runBlocking(DesktopFrameGovernor) {
        awaitApplication(content = content)
    }
    exitProcess(0)
}
