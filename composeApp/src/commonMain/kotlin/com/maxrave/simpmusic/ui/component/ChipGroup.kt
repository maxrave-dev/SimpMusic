package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
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
                colors =
                    FilterChipDefaults.elevatedFilterChipColors(
                        containerColor = Color.Transparent,
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