package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.extension.greyScale
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.ui.theme.white

@Composable
fun SettingItem(
    title: String = "Title",
    subtitle: String = "Subtitle",
    smallSubtitle: Boolean = false,
    isEnable: Boolean = true,
    onClick: (() -> Unit)? = null,
    switch: Pair<Boolean, ((Boolean) -> Unit)>? = null,
    onDisable: (() -> Unit)? = null, // Callback when the item is disabled, switch off settings
    otherView: @Composable (() -> Unit)? = null,
) {
    LaunchedEffect(Unit) {
        if (!isEnable && onDisable != null) {
            onDisable.invoke()
        }
    }
    Box(
        Modifier
            .then(
                if (onClick != null && isEnable) {
                    Modifier.clickable { onClick.invoke() }
                } else {
                    Modifier
                },
            ).then(
                if (!isEnable) {
                    Modifier.greyScale()
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 8.dp,
                        horizontal = 24.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = title,
                    style =
                        typo().labelMedium.let {
                            if (!isEnable) it.greyScale() else it
                        },
                    color = white,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style =
                        if (smallSubtitle) {
                            typo().bodySmall.let {
                                if (!isEnable) it.greyScale() else it
                            }
                        } else {
                            typo().bodyMedium.let {
                                if (!isEnable) it.greyScale() else it
                            }
                        },
                    maxLines = 2,
                )

                otherView?.let {
                    Spacer(Modifier.height(16.dp))
                    it.invoke()
                }
            }
            if (switch != null) {
                Spacer(Modifier.width(10.dp))
                Switch(
                    modifier = Modifier.wrapContentWidth(),
                    checked = switch.first,
                    onCheckedChange = {
                        switch.second.invoke(it)
                    },
                    enabled = isEnable,
                )
            }
        }
    }
}