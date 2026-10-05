package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import com.maxrave.simpmusic.ui.component.LoginSyncHostDialog

@Composable
actual fun LoginSyncDialog(onDismiss: () -> Unit) = LoginSyncHostDialog(onDismiss)
