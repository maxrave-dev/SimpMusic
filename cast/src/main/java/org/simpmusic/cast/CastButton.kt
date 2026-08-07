package org.simpmusic.cast

import android.view.ContextThemeWrapper
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.R as MediaRouterR
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory

/**
 * Cast icon button. No-op (renders nothing) when Cast isn't available.
 *
 * MediaRouteButton requires an AppCompat-derived theme to inflate correctly;
 * the host Activity/Application theme in this Compose-first app isn't
 * guaranteed to be one, so the factory context is wrapped with mediarouter's
 * own `Theme.MediaRouter` to avoid a "You need to use a Theme.AppCompat theme"
 * crash.
 *
 * [tint] recolours the icon so callers can signal an active session.
 */
@Composable
fun CastIconButton(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
) {
    if (!isCastAvailable()) return

    val context = LocalContext.current
    val tintArgb = tint.toArgb()

    // Keyed on the tint so the drawable is rebuilt only when the colour actually changes. The host
    // screen recomposes on every playback-progress tick (~10x/sec); rebuilding here would hammer
    // setRemoteIndicatorDrawable at that rate, and each call unschedules the old drawable and
    // refreshes the button's drawable state — enough to trample the press state and swallow taps.
    //
    // mutate() is essential: AppCompatResources hands back a drawable sharing its ConstantState,
    // so tinting it without mutating would recolour every other user of this resource.
    val indicator =
        remember(context, tintArgb) {
            AppCompatResources
                .getDrawable(context, R.drawable.ic_music_cast)
                ?.mutate()
                ?.apply { setTint(tintArgb) }
        }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val themedContext = ContextThemeWrapper(context, MediaRouterR.style.Theme_MediaRouter)
            MediaRouteButton(themedContext).apply {
                CastButtonFactory.setUpMediaRouteButton(context.applicationContext, this)
                // MediaRouteButton inherits Widget.AppCompat.ActionButton, which bakes in 12dp of
                // horizontal padding and scaleType=center (the drawable is never scaled, only
                // centred and clipped). Callers size this to match the 24dp icon buttons it sits
                // next to, and 24dp - 12dp - 12dp leaves a 0dp content rect: the glyph would
                // vanish completely. Dropping the padding lets the 24dp drawable fill the box.
                setPadding(0, 0, 0, 0)
            }
        },
        update = { button ->
            // Set here rather than in factory() so a tint change actually reaches the view, and
            // after setUpMediaRouteButton, which installs its own indicator. [indicator] is
            // remembered per-tint, so this is a no-op assignment on ordinary recompositions.
            //
            // This swaps MediaRouteButton's state-list AVD for one static glyph, trading away the
            // "connecting" animation and the built-in connected/disconnected distinction — the
            // caller conveys session state through [tint] instead. Tapping still opens the route
            // chooser when idle and the controller dialog (with "Stop casting") when connected.
            button.setRemoteIndicatorDrawable(indicator)
        },
    )
}
