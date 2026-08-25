package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Chip(
    isAnimated: Boolean = false,
    isSelected: Boolean = false,
    text: String,
    onClick: () -> Unit,
) {
    InfiniteBorderAnimationView(
        isAnimated = isAnimated && isSelected,
        brush = Brush.sweepGradient(listOf(Color.Gray, Color.White)),
        backgroundColor = Color.Transparent,
        contentPadding = 0.dp,
        borderWidth = 1.dp,
        shape = CircleShape,
        oneCircleDurationMillis = 2500,
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            ElevatedFilterChip(
                shape = CircleShape,
                // No shadow. An unselected chip has no fill, and a drop shadow under a see-through
                // shape shows THROUGH it as a dark ring instead of sitting behind it. Naming
                // `elevation` alone is enough: pressed, focused and hovered all default to it.
                elevation = FilterChipDefaults.elevatedFilterChipElevation(elevation = 0.dp),
                colors =
                    FilterChipDefaults.elevatedFilterChipColors(
                        // Unselected chips have no fill at all. They mostly sit over artwork (Home)
                        // where a solid pill reads as a slab punched over the image; the outline
                        // below is what carries the shape now. The selected chip deliberately stays
                        // opaque — that contrast IS the selection, and it is the only one that has
                        // to be found at a glance.
                        containerColor = Color.Transparent,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        // `primary` is a pale #B2C5FF on the dark theme, so the label has to be its
                        // matching `on` token — a hand-picked white would read on one theme and
                        // vanish on the other.
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                onClick = { onClick.invoke() },
                label = {
                    Text(text, maxLines = 1)
                },
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        selectedBorderColor = Color.Transparent,
                        borderColor = MaterialTheme.colorScheme.outline,
                    ),
                // No check icon. A solid `primary` fill against a fully transparent unselected chip
                // is already the strongest contrast available, so a second signal adds nothing — and
                // it cost real usability: the icon made the selected chip ~26dp wider than itself, so
                // every tap reflowed the rest of the scrolling row sideways under the finger that had
                // just tapped it. Material ships the tick because its own default selected fill
                // (`secondaryContainer`) is too faint to stand alone; that is not the case here.
                // Selection also survives without colour vision, being filled-versus-empty rather
                // than one hue against another.
                selected = isSelected,
            )
        }
    }
}