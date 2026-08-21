package com.maxrave.simpmusic.ui.ext

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

fun Modifier.hapticClickable(
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) = composed {
    val haptic = LocalHapticFeedback.current
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val actualIndication = indication ?: LocalIndication.current

    clickable(
        interactionSource = actualInteractionSource,
        indication = actualIndication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.hapticCombinedClickable(
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed {
    val haptic = LocalHapticFeedback.current
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val actualIndication = indication ?: LocalIndication.current

    combinedClickable(
        interactionSource = actualInteractionSource,
        indication = actualIndication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onLongClickLabel = onLongClickLabel,
        onLongClick = onLongClick?.let {
            {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                it()
            }
        },
        onDoubleClick = onDoubleClick,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    )
}
