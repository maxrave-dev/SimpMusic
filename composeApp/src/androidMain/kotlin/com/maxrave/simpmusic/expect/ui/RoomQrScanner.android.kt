package com.maxrave.simpmusic.expect.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.maxrave.simpmusic.extension.findActivity
import com.maxrave.simpmusic.ui.component.QrScanner
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.roomCodeFromText
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.login_sync_allow_camera
import simpmusic.composeapp.generated.resources.login_sync_no_camera
import simpmusic.composeapp.generated.resources.login_sync_open_settings
import simpmusic.composeapp.generated.resources.lt_scan_camera_blocked
import simpmusic.composeapp.generated.resources.lt_scan_camera_needed
import simpmusic.composeapp.generated.resources.lt_scan_hint
import simpmusic.composeapp.generated.resources.lt_scan_not_a_room
import simpmusic.composeapp.generated.resources.lt_scan_qr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun RoomQrScanner(
    onRoomCode: (String) -> Unit,
    onUnreadable: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = rememberSurfaceDarkColors()

    fun hasCamera() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    var cameraAllowed by remember { mutableStateOf(hasCamera()) }
    var cameraBlocked by remember { mutableStateOf(false) }
    var cameraUnavailable by remember { mutableStateOf(false) }
    var notARoom by remember { mutableStateOf(false) }
    // The camera keeps reporting the same code until the sheet has finished sliding away.
    var finished by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            cameraAllowed = granted
            // Rationale is false after a denial only when the system will not ask again.
            cameraBlocked = !granted && !context.findActivity().shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        }

    // Back from the system settings page with the camera now allowed.
    LifecycleResumeEffect(Unit) {
        if (hasCamera()) cameraAllowed = true
        onPauseOrDispose { }
    }

    fun hideThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors().copy(containerColor = colors.container),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .width(60.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.handle),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.lt_scan_qr),
                        style = typo().titleMedium,
                        color = colors.content,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { hideThen(onDismiss) }) {
                        Icon(imageVector = SimpIcons.Close, contentDescription = stringResource(Res.string.cancel), tint = colors.content)
                    }
                }
                Text(
                    text = stringResource(if (notARoom) Res.string.lt_scan_not_a_room else Res.string.lt_scan_hint),
                    style = typo().bodySmall,
                    color = if (notARoom) MaterialTheme.colorScheme.error else colors.subtitle,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(20.dp))
                val scanning = cameraAllowed && !cameraUnavailable
                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 24.dp)
                            // Square on a phone; capped so it still fits a phone held sideways.
                            .widthIn(max = 340.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (scanning) Color.Black else colors.disabled.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        cameraUnavailable -> {
                            Status(stringResource(Res.string.login_sync_no_camera), progress = false)
                        }

                        cameraAllowed -> {
                            QrScanner(
                                onResult = { text ->
                                    val code = roomCodeFromText(text)
                                    if (code == null) {
                                        notARoom = true
                                    } else if (!finished) {
                                        finished = true
                                        hideThen { onRoomCode(code) }
                                    }
                                },
                                onCameraUnavailable = { cameraUnavailable = true },
                                modifier = Modifier.fillMaxSize(),
                            )
                            ViewfinderCorners(Modifier.fillMaxSize().padding(20.dp))
                        }

                        cameraBlocked -> {
                            Status(stringResource(Res.string.lt_scan_camera_blocked), progress = false) {
                                SheetButton(stringResource(Res.string.login_sync_open_settings)) { context.openAppSettings() }
                            }
                        }

                        else -> {
                            Status(stringResource(Res.string.lt_scan_camera_needed), progress = false) {
                                SheetButton(stringResource(Res.string.login_sync_allow_camera)) {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
