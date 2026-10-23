package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.highlight.Highlight
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toColor
import com.materialkolor.ktx.toHct
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled

@Composable
fun Chip(
    isAnimated: Boolean = false,
    isSelected: Boolean = false,
    text: String,
    // A row floating over scrolling content (Home, Library, Search) passes the backdrop its content
    // records into, and its unselected chips turn to liquid glass while the setting is on. The selected
    // chip stays a solid `primary` fill, as in Spotify's row. Chips on a flat card or sheet pass
    // nothing: glass over a flat surface reads as a mere border.
    backdrop: PlatformBackdrop? = null,
    // A smaller chip for dense panels such as the Desktop sidebar: 28dp tall with a 12sp label.
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val glassOn = LocalLiquidGlassEnabled.current
    val glassBackdrop = backdrop?.takeIf { glassOn && !isSelected }
    ChipFrame(isAnimated = isAnimated && isSelected) {
        ChipBody(
            text = text,
            selected = isSelected,
            // The 1dp uniform rim every small glass control here carries: the default rim lights one
            // side only, which on a chip this small does not show at all.
            modifier = if (glassBackdrop != null) Modifier.liquidGlass(glassBackdrop, highlight = Highlight(width = 1.dp)) else Modifier,
            // outlineVariant, per M3's Aug 2024 chip update: a softer stroke than outline.
            // Glass draws its own rim, so the outline goes.
            borderColor = if (glassBackdrop != null) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
            onClick = onClick,
            compact = compact,
        )
    }
}

/**
 * Spotify's collapsing filter. Closed it is one chip ([text]); tapped, it opens into its [options]
 * inside a single pill led by [text]; once an option is picked it folds back into one pill reading
 * "[text] | option", the [text] half a brighter capsule sitting inside it.
 *
 * Every piece is the size of a [Chip]: the group goes through the same [ChipFrame], and each part of
 * it is the same [ChipBody], so the pill lines up with the plain chips beside it to the pixel.
 */
@Composable
fun ExpandableChip(
    text: String,
    options: List<String>,
    selectedIndex: Int?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit,
    isAnimated: Boolean = false,
    backdrop: PlatformBackdrop? = null,
) {
    val picked = selectedIndex?.let(options::getOrNull)
    // The folded label is still on screen while it shrinks away after the pick is cleared (All), so it
    // shows the last pick rather than nothing.
    var lastPicked by remember { mutableStateOf(picked) }
    SideEffect { if (picked != null) lastPicked = picked }
    val pickedLabel = picked ?: lastPicked.orEmpty()

    // One structure for all three states, so every change between them animates instead of swapping
    // one layout for another: closed is the [text] chip alone, open adds the options after it, and
    // folded adds the pick after it on a darker fill.
    val folded = !expanded && picked != null
    val active = expanded || picked != null
    val colorScheme = MaterialTheme.colorScheme
    val glassOn = LocalLiquidGlassEnabled.current
    val glassBackdrop = backdrop?.takeIf { glassOn }
    // A darker shade of primary: the same hue and chroma, 20 tones lower. Mixing primary with black
    // instead only greys it out, which reads as faded rather than darker.
    val primaryHct = colorScheme.primary.toHct()
    val foldedFill = Hct.from(primaryHct.hue, primaryHct.chroma, (primaryHct.tone - 20).coerceAtLeast(0.0)).toColor()
    val fill by animateColorAsState(if (folded) foldedFill else Color.Transparent)
    // Glass draws its own rim; without it the pill is outlined like an unselected chip until it folds.
    val outline by animateColorAsState(
        if (glassBackdrop == null && !folded) colorScheme.outlineVariant else Color.Transparent,
    )
    // Folded, the pill is a selected chip, and a selected chip has no rim (the All chip beside it has
    // none). The rim is drawn over the content, so the solid fill cannot hide it; it fades out instead.
    val rim by animateFloatAsState(if (folded) 0f else 1f)
    val headFill by animateColorAsState(if (active) colorScheme.primary else Color.Transparent)
    val headLabel by animateColorAsState(if (active) colorScheme.onPrimary else colorScheme.onSurfaceVariant)
    ChipFrame(isAnimated = isAnimated && picked != null) {
        Row(
            modifier =
                Modifier
                    .then(
                        if (glassBackdrop != null) {
                            // The press swell is sized for a chip, so only the closed chip keeps it.
                            Modifier.liquidGlass(
                                glassBackdrop,
                                interactive = !active,
                                highlight = Highlight(width = 1.dp, alpha = rim),
                            )
                        } else {
                            Modifier.clip(CircleShape)
                        },
                    ).background(fill, CircleShape)
                    .border(1.dp, outline, CircleShape),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChipBody(
                text = text,
                selected = false,
                containerColor = headFill,
                labelColor = headLabel,
                borderColor = Color.Transparent,
                onClick = { onExpandedChange(!expanded) },
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = expanded,
                enter = expandHorizontally() + fadeIn(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    options.forEachIndexed { index, option ->
                        val isPicked = index == selectedIndex
                        ChipBody(
                            text = option,
                            selected = false,
                            containerColor = if (isPicked) colorScheme.onSurface.copy(alpha = 0.14f) else Color.Transparent,
                            labelColor = if (isPicked) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                            borderColor = Color.Transparent,
                            onClick = { onSelect(index) },
                        )
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = folded,
                enter = expandHorizontally() + fadeIn(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                ChipBody(
                    text = pickedLabel,
                    selected = false,
                    labelColor = colorScheme.onPrimary,
                    borderColor = Color.Transparent,
                    onClick = { onExpandedChange(true) },
                )
            }
        }
    }
}

/**
 * The loading ring around a chip. It also pads the chip by its 1dp border width, so anything that has
 * to be the size of a chip goes through here too.
 */
@Composable
private fun ChipFrame(
    isAnimated: Boolean,
    content: @Composable () -> Unit,
) {
    InfiniteBorderAnimationView(
        isAnimated = isAnimated,
        brush = Brush.sweepGradient(listOf(Color.Gray, Color.White)),
        backgroundColor = Color.Transparent,
        contentPadding = 0.dp,
        borderWidth = 1.dp,
        shape = CircleShape,
        oneCircleDurationMillis = 2500,
        content = content,
    )
}

@Composable
private fun ChipBody(
    text: String,
    selected: Boolean,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Unselected chips have no fill at all. They mostly sit over artwork (Home) where a solid pill
    // reads as a slab punched over the image; the outline is what carries the shape now. The selected
    // chip deliberately stays opaque — that contrast IS the selection, and it is the only one that has
    // to be found at a glance.
    containerColor: Color = Color.Transparent,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    compact: Boolean = false,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        ElevatedFilterChip(
            modifier = if (compact) modifier.height(28.dp) else modifier,
            shape = CircleShape,
            // No shadow. An unselected chip has no fill, and a drop shadow under a see-through
            // shape shows THROUGH it as a dark ring instead of sitting behind it. Naming
            // `elevation` alone is enough: pressed, focused and hovered all default to it.
            elevation = FilterChipDefaults.elevatedFilterChipElevation(elevation = 0.dp),
            colors =
                FilterChipDefaults.elevatedFilterChipColors(
                    containerColor = containerColor,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    labelColor = labelColor,
                    // `primary` is a pale #B2C5FF on the dark theme, so the label has to be its
                    // matching `on` token — a hand-picked white would read on one theme and
                    // vanish on the other.
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            onClick = onClick,
            label = {
                // Only the size changes: the chip's own label style still supplies weight and colour.
                Text(
                    text,
                    maxLines = 1,
                    style = if (compact) LocalTextStyle.current.copy(fontSize = 12.sp, lineHeight = 16.sp) else LocalTextStyle.current,
                )
            },
            border =
                FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    selectedBorderColor = Color.Transparent,
                    borderColor = borderColor,
                ),
            // No check icon. A solid `primary` fill against a fully transparent unselected chip
            // is already the strongest contrast available, so a second signal adds nothing — and
            // it cost real usability: the icon made the selected chip ~26dp wider than itself, so
            // every tap reflowed the rest of the scrolling row sideways under the finger that had
            // just tapped it. Material ships the tick because its own default selected fill
            // (`secondaryContainer`) is too faint to stand alone; that is not the case here.
            // Selection also survives without colour vision, being filled-versus-empty rather
            // than one hue against another.
            selected = selected,
        )
    }
}
