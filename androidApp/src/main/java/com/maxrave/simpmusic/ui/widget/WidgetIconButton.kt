package com.maxrave.simpmusic.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.maxrave.simpmusic.R

/**
 * A round icon button for the widgets, without the tap ripple.
 *
 * Glance's own `CircleIconButton` has no way to turn the ripple off — it takes no
 * `rippleOverride`, and `NoRippleOverride` (0) means "keep the default" rather than "none". The
 * only lever is `clickable(rippleOverride = …)`, so the button is assembled by hand here and
 * pointed at a fully transparent drawable.
 */
@Composable
fun WidgetIconButton(
    provider: ImageProvider,
    contentDescription: String,
    tint: Color,
    background: Color,
    size: Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            GlanceModifier
                .size(size)
                .background(ColorProvider(background))
                .cornerRadius(size / 2)
                // Trailing lambda, not `onClick = …`. Both clickable overloads name that
                // parameter `onClick`, so passing it by name resolves to the one taking an
                // Action; the trailing form picks the () -> Unit overload.
                .clickable(rippleOverride = R.drawable.no_ripple) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = provider,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(ColorProvider(tint)),
            modifier = GlanceModifier.size(size * 0.58f),
        )
    }
}
