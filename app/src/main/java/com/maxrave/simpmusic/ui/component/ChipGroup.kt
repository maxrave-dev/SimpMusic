package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.theme.colorPrimaryDark

@Composable
fun Chip(
    isAnimated: Boolean = false,
    isSelected: Boolean = false,
    text: String,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Infinite Color Animation")
    val degrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "Infinite Colors",
    )
    Surface(
        modifier =
            Modifier
                .clip(
                    RoundedCornerShape(12.dp),
                ).padding(1.dp)
                .drawBehind {
                    if (isSelected && isAnimated) {
                        rotate(degrees = degrees) {
                            drawCircle(
                                brush = Brush.sweepGradient(listOf(Color.Gray, Color.White)),
                                radius = size.width,
                                blendMode = BlendMode.SrcIn,
                            )
                        }
                    }
                },
        shape = RoundedCornerShape(12.dp),
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            ElevatedFilterChip(
                shape = RoundedCornerShape(12.dp),
                colors =
                    FilterChipDefaults.elevatedFilterChipColors(
                        containerColor = colorPrimaryDark,
                        iconColor = Color.White,
                        selectedContainerColor = Color.DarkGray.copy(alpha = 0.8f),
                        labelColor = Color.LightGray,
                        selectedLabelColor = Color.LightGray,
                    ),
                onClick = { onClick.invoke() },
                label = {
                    Text(text)
                },
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        selectedBorderColor = Color.Transparent,
                        borderColor = Color.Gray.copy(alpha = 0.8f),
                    ),
                selected = isSelected,
                leadingIcon =
                    if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = "Done icon",
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
            )
        }
    }
}