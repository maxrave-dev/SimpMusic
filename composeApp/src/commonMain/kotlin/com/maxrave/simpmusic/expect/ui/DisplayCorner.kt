package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * The corner radius of the device's screen, so a floating sheet can nest its corners inside it the
 * way Apple's sheets do. Null where the platform does not say (Android before 12, Desktop).
 */
@Composable
expect fun rememberDisplayCornerRadius(): Dp?
