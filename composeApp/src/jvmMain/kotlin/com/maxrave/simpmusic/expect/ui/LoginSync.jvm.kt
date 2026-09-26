package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.maxrave.domain.data.model.loginsync.LoginSyncHostState
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LoginSyncHostViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.login_sync_android_title
import simpmusic.composeapp.generated.resources.login_sync_desktop_instructions
import simpmusic.composeapp.generated.resources.login_sync_desktop_title
import simpmusic.composeapp.generated.resources.login_sync_expired
import simpmusic.composeapp.generated.resources.login_sync_new_code
import simpmusic.composeapp.generated.resources.login_sync_no_network
import simpmusic.composeapp.generated.resources.login_sync_phone_connected
import simpmusic.composeapp.generated.resources.login_sync_pick_address
import simpmusic.composeapp.generated.resources.login_sync_receive_failed
import simpmusic.composeapp.generated.resources.login_sync_received
import simpmusic.composeapp.generated.resources.login_sync_start_failed
import simpmusic.composeapp.generated.resources.ok

@Composable
actual fun LoginSyncDialog(onDismiss: () -> Unit) {
    val viewModel: LoginSyncHostViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.open() }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }

    val canRestart =
        state is LoginSyncHostState.Expired ||
            state is LoginSyncHostState.NoNetwork ||
            state is LoginSyncHostState.Failed ||
            state is LoginSyncHostState.Rejected
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.login_sync_desktop_title), style = typo().titleSmall) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (val s = state) {
                    LoginSyncHostState.Starting -> {
                        CircularProgressIndicator()
                    }

                    is LoginSyncHostState.Waiting -> {
                        QrCode(s.invite, Modifier.size(240.dp))
                        Text(
                            text = stringResource(Res.string.login_sync_desktop_instructions, stringResource(Res.string.login_sync_android_title)),
                            style = typo().bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(Res.string.login_sync_pick_address),
                            style = typo().bodySmall,
                            textAlign = TextAlign.Center,
                        )
                        Column(Modifier.fillMaxWidth()) {
                            s.addresses.forEach { address ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .selectable(
                                                selected = address == s.selected,
                                                role = Role.RadioButton,
                                                onClick = { viewModel.select(address) },
                                            ).padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(selected = address == s.selected, onClick = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = address.ip, style = typo().bodyMedium)
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = address.interfaceName, style = typo().bodySmall, maxLines = 1)
                                }
                            }
                        }
                    }

                    LoginSyncHostState.Connected -> {
                        CircularProgressIndicator()
                        Text(text = stringResource(Res.string.login_sync_phone_connected), textAlign = TextAlign.Center)
                    }

                    is LoginSyncHostState.Done -> {
                        Text(text = stringResource(Res.string.login_sync_received, s.services.joinToString { it.label }))
                    }

                    LoginSyncHostState.Expired -> {
                        Text(text = stringResource(Res.string.login_sync_expired))
                    }

                    LoginSyncHostState.NoNetwork -> {
                        Text(text = stringResource(Res.string.login_sync_no_network))
                    }

                    is LoginSyncHostState.Failed -> {
                        Text(text = stringResource(Res.string.login_sync_start_failed, s.message))
                    }

                    is LoginSyncHostState.Rejected -> {
                        Text(text = stringResource(Res.string.login_sync_receive_failed, s.reason), textAlign = TextAlign.Center)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(if (state is LoginSyncHostState.Done) Res.string.ok else Res.string.cancel))
            }
        },
        dismissButton =
            if (canRestart) {
                {
                    TextButton(onClick = viewModel::open) {
                        Text(text = stringResource(Res.string.login_sync_new_code))
                    }
                }
            } else {
                null
            },
    )
}

/** Dark modules on white whatever the theme: scanners expect that polarity. */
@Composable
private fun QrCode(
    text: String,
    modifier: Modifier = Modifier,
) {
    val matrix =
        remember(text) {
            QRCodeWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                0,
                0,
                mapOf(EncodeHintType.MARGIN to 2, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
            )
        }
    Canvas(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(8.dp),
    ) {
        val cell = size.minDimension / matrix.width
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                // Half a pixel of overlap hides the seams between fractional-width modules.
                if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}
