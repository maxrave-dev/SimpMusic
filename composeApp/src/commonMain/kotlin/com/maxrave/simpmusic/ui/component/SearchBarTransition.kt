package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

/**
 * How the search bar enters and leaves, shared by every screen that has one.
 *
 * The offset is `-it`, a full height rather than the default `-it / 2`: at half a height the bar
 * appears already halfway down and the first part of the movement is missing, which reads as a
 * jump rather than as something sliding in from above.
 *
 * Fade and slide run on different durations on purpose — the bar is fully opaque before it stops
 * moving, so it settles into place instead of arriving and then finishing its fade.
 */
val SearchBarEnter: EnterTransition =
    fadeIn(tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
        slideInVertically(tween(durationMillis = 260, easing = FastOutSlowInEasing)) { -it }

/** Leaving is quicker than arriving: a control being dismissed should not hold the eye. */
val SearchBarExit: ExitTransition =
    fadeOut(tween(durationMillis = 140, easing = FastOutSlowInEasing)) +
        slideOutVertically(tween(durationMillis = 200, easing = FastOutSlowInEasing)) { -it }
