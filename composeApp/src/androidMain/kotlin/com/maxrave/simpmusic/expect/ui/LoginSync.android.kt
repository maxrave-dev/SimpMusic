package com.maxrave.simpmusic.expect.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.data.model.loginsync.LoginSyncException
import com.maxrave.simpmusic.extension.findActivity
import com.maxrave.simpmusic.ui.component.QrScanner
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LoginSyncSenderViewModel
import com.maxrave.simpmusic.viewModel.LoginSyncSenderViewModel.Step
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.login_sync_allow_camera
import simpmusic.composeapp.generated.resources.login_sync_android_title
import simpmusic.composeapp.generated.resources.login_sync_camera_blocked
import simpmusic.composeapp.generated.resources.login_sync_camera_needed
import simpmusic.composeapp.generated.resources.login_sync_choose_title
import simpmusic.composeapp.generated.resources.login_sync_code_expired
import simpmusic.composeapp.generated.resources.login_sync_connecting
import simpmusic.composeapp.generated.resources.login_sync_failed
import simpmusic.composeapp.generated.resources.login_sync_no_camera
import simpmusic.composeapp.generated.resources.login_sync_nothing_signed_in
import simpmusic.composeapp.generated.resources.login_sync_open_settings
import simpmusic.composeapp.generated.resources.login_sync_scan_hint
import simpmusic.composeapp.generated.resources.login_sync_send
import simpmusic.composeapp.generated.resources.login_sync_sending
import simpmusic.composeapp.generated.resources.login_sync_sent
import simpmusic.composeapp.generated.resources.login_sync_trust
import simpmusic.composeapp.generated.resources.login_sync_trust_message
import simpmusic.composeapp.generated.resources.login_sync_trust_title
import simpmusic.composeapp.generated.resources.login_sync_unreachable
import simpmusic.composeapp.generated.resources.ok
import simpmusic.composeapp.generated.resources.retry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun LoginSyncDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val viewModel: LoginSyncSenderViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val step = uiState.step
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = rememberSurfaceDarkColors()

    // Permissions belong to the Activity, so they are asked here and only the answer goes to the VM.
    val permissions = remember { loginSyncPermissions(context) }

    fun hasPermissions() = permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            // An interrupted request comes back as an EMPTY map, and all{} on nothing is true.
            val granted = result.isNotEmpty() && result.values.all { it }
            // Rationale is false after a denial only when the system will not ask again.
            val blocked = !granted && !context.findActivity().shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            viewModel.onCameraPermission(granted, blocked)
        }

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    LaunchedEffect(viewModel) { viewModel.open(hasCamera = hasPermissions()) }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    // Back from the system settings page with the camera now allowed.
    LifecycleResumeEffect(step) {
        if (step is Step.NeedsCamera && hasPermissions()) viewModel.onCameraPermission(granted = true, blocked = false)
        onPauseOrDispose { }
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
                        text = stringResource(Res.string.login_sync_android_title),
                        style = typo().titleMedium,
                        color = colors.content,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = ::close) {
                        Icon(imageVector = SimpIcons.Close, contentDescription = stringResource(Res.string.cancel), tint = colors.content)
                    }
                }
                Text(
                    text = stringResource(Res.string.login_sync_scan_hint),
                    style = typo().bodySmall,
                    color = colors.subtitle,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 24.dp)
                            // Square on a phone; capped so it still fits a phone held sideways.
                            .widthIn(max = 340.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (step == Step.Scanning) Color.Black else colors.disabled.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    when (step) {
                        Step.Scanning -> {
                            QrScanner(
                                onResult = viewModel::onScanned,
                                onCameraUnavailable = viewModel::onCameraUnavailable,
                                modifier = Modifier.fillMaxSize(),
                            )
                            ViewfinderCorners(Modifier.fillMaxSize().padding(20.dp))
                        }

                        Step.Loading -> {
                            CircularProgressIndicator()
                        }

                        Step.Connecting -> {
                            Status(stringResource(Res.string.login_sync_connecting), progress = true)
                        }

                        Step.Sending -> {
                            Status(stringResource(Res.string.login_sync_sending), progress = true)
                        }

                        is Step.Trust -> {
                            Status("${step.desktop.name} · ${step.desktop.os}", progress = false)
                        }

                        is Step.Choose -> {
                            Status("${step.desktop.name} · ${step.desktop.os}", progress = false)
                        }

                        is Step.NeedsCamera -> {
                            if (step.blocked) {
                                Status(stringResource(Res.string.login_sync_camera_blocked), progress = false) {
                                    SheetButton(stringResource(Res.string.login_sync_open_settings)) { context.openAppSettings() }
                                }
                            } else {
                                Status(stringResource(Res.string.login_sync_camera_needed), progress = false) {
                                    SheetButton(stringResource(Res.string.login_sync_allow_camera)) {
                                        permissionLauncher.launch(permissions.toTypedArray())
                                    }
                                }
                            }
                        }

                        Step.NoCamera -> {
                            Status(stringResource(Res.string.login_sync_no_camera), progress = false)
                        }

                        Step.NothingSignedIn -> {
                            Status(stringResource(Res.string.login_sync_nothing_signed_in), progress = false)
                        }

                        is Step.Done -> {
                            Status(
                                stringResource(Res.string.login_sync_sent, step.desktop.name, step.services.joinToString { it.label }),
                                progress = false,
                            ) {
                                SheetButton(stringResource(Res.string.ok), ::close)
                            }
                        }

                        is Step.Failed -> {
                            val message =
                                when (step.reason) {
                                    LoginSyncException.Reason.UNREACHABLE -> Res.string.login_sync_unreachable
                                    LoginSyncException.Reason.CODE_EXPIRED -> Res.string.login_sync_code_expired
                                    LoginSyncException.Reason.FAILED -> Res.string.login_sync_failed
                                }
                            Status(stringResource(message) + step.detail?.let { "\n\n$it" }.orEmpty(), progress = false) {
                                SheetButton(stringResource(Res.string.retry)) { viewModel.retry(hasCamera = hasPermissions()) }
                            }
                        }
                    }
                }
            }
        }
    }

    when (step) {
        is Step.Trust -> {
            AlertDialog(
                onDismissRequest = ::close,
                title = { Text(text = stringResource(Res.string.login_sync_trust_title), style = typo().titleSmall) },
                text = { Text(text = stringResource(Res.string.login_sync_trust_message, step.desktop.name, step.desktop.os)) },
                confirmButton = {
                    TextButton(onClick = viewModel::trust) { Text(text = stringResource(Res.string.login_sync_trust)) }
                },
                dismissButton = {
                    TextButton(onClick = ::close) { Text(text = stringResource(Res.string.cancel)) }
                },
            )
        }

        is Step.Choose -> {
            AlertDialog(
                onDismissRequest = ::close,
                title = { Text(text = stringResource(Res.string.login_sync_choose_title), style = typo().titleSmall) },
                text = {
                    Column {
                        uiState.signedIn.forEach { service ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .toggleable(
                                            value = service in uiState.selected,
                                            role = Role.Checkbox,
                                            onValueChange = { viewModel.toggle(service) },
                                        ).padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Checkbox(checked = service in uiState.selected, onCheckedChange = null)
                                Text(text = service.label, style = typo().bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(enabled = uiState.selected.isNotEmpty(), onClick = viewModel::send) {
                        Text(text = stringResource(Res.string.login_sync_send))
                    }
                },
                dismissButton = {
                    TextButton(onClick = ::close) { Text(text = stringResource(Res.string.cancel)) }
                },
            )
        }

        else -> {}
    }
}

/** A message centred in the viewfinder square, with an optional spinner above and action below. */
@Composable
private fun Status(
    text: String,
    progress: Boolean,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (progress) CircularProgressIndicator()
        Text(
            text = text,
            style = typo().bodyMedium,
            color = rememberSurfaceDarkColors().content,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

/**
 * The app's sheet button (ModalBottomSheet.kt's sleep timer "Set"): seed fill, 12dp corners. The text
 * colour is set explicitly because typo() bakes a body colour into every style — a stock M3 Button
 * inherits that instead of onPrimary and renders pale text on a pale fill.
 */
@Composable
private fun SheetButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = seed),
    ) {
        Text(
            text = text,
            style = typo().labelSmall,
            color = rememberSurfaceDarkColors().content,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

/** Four corner brackets marking where the code should sit. */
@Composable
private fun ViewfinderCorners(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val arm = size.minDimension * 0.14f
        val stroke = 4.dp.toPx()
        val corners =
            listOf(
                Offset(0f, 0f) to Offset(1f, 1f),
                Offset(size.width, 0f) to Offset(-1f, 1f),
                Offset(0f, size.height) to Offset(1f, -1f),
                Offset(size.width, size.height) to Offset(-1f, -1f),
            )
        corners.forEach { (corner, direction) ->
            drawLine(Color.White, corner, corner + Offset(direction.x * arm, 0f), stroke, StrokeCap.Round)
            drawLine(Color.White, corner, corner + Offset(0f, direction.y * arm), stroke, StrokeCap.Round)
        }
    }
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/**
 * Camera always. Android 17 also gates LAN sockets behind a runtime permission for apps targeting
 * 37+, and without it the connection does not fail — it times out (element-x-android#7694). While
 * targetSdk is 36 there is nothing to ask for, but the check is here so a targetSdk bump cannot
 * silently break this.
 */
private fun loginSyncPermissions(context: Context): List<String> =
    buildList {
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 37 && context.applicationInfo.targetSdkVersion >= 37) {
            add("android.permission.ACCESS_LOCAL_NETWORK")
        }
    }
