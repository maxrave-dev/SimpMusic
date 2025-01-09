package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.theme.colorPrimaryDark

@Composable
fun Chip(
    isSelected: Boolean = false,
    text: String,
    onClick: () -> Unit,
) {
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