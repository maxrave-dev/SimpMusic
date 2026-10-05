package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.maxrave.simpmusic.isTv

/**
 * `composable<T>` for every destination in the app. On a TV the destination is one focus group
 * that takes focus when it appears and, when the user comes back to it, hands focus back to the
 * item they left from. Everywhere else it is plain `composable<T>`.
 */
inline fun <reified T : Any> NavGraphBuilder.tvComposable(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T> { entry ->
        if (isTv()) {
            TvFocusDestination { content(entry) }
        } else {
            content(entry)
        }
    }
}

/**
 * `focusRestorer` writes the child that had focus into the destination's own saved state, so the
 * choice survives the destination leaving composition while another one is on top of it. It only
 * remembers its DIRECT child, though: reaching the item itself takes a restorer on every focus
 * group on the way down, which is why the browse screens' lazy lists carry `focusRestorer()` too.
 * The child is saved when focus leaves the group, and that happens here because the next
 * destination asks for focus while this one is still composed under the exit transition.
 */
@Composable
fun TvFocusDestination(content: @Composable () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    Box(Modifier.focusRequester(focusRequester).focusRestorer().focusGroup()) {
        content()
    }
    LaunchedEffect(Unit) {
        // Lazy lists compose their items in the layout pass, which runs after this effect starts,
        // so wait for a frame before asking. If the page still has nothing focusable a few frames
        // in, the first D-pad press finds it anyway.
        repeat(10) {
            withFrameNanos { }
            if (focusRequester.requestFocus(FocusDirection.Enter)) return@LaunchedEffect
        }
    }
}

/**
 * For tap targets that only repeat a control the screen already has — the surfaces that toggle
 * the player's controls, lyric lines, captions. They draw nothing when focused, so on a TV they
 * would be stops the D-pad lands on without the user seeing where it went. Taps still work.
 */
fun Modifier.skipFocusOnTv(): Modifier = if (isTv()) focusProperties { canFocus = false } else this
