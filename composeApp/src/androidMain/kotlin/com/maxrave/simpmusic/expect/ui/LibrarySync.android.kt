package com.maxrave.simpmusic.expect.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.data.model.librarysync.LibrarySyncProblem
import com.maxrave.domain.data.model.librarysync.LibrarySyncState
import com.maxrave.simpmusic.extension.findActivity
import com.maxrave.simpmusic.ui.component.QrScanner
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibrarySyncPhoneViewModel
import com.maxrave.simpmusic.viewModel.LibrarySyncPhoneViewModel.Step
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.mp.KoinPlatform.getKoin
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.library_sync_allow
import simpmusic.composeapp.generated.resources.library_sync_computer
import simpmusic.composeapp.generated.resources.library_sync_failed
import simpmusic.composeapp.generated.resources.library_sync_first_sync_phone
import simpmusic.composeapp.generated.resources.library_sync_last_synced
import simpmusic.composeapp.generated.resources.library_sync_needs_nearby
import simpmusic.composeapp.generated.resources.library_sync_pair
import simpmusic.composeapp.generated.resources.library_sync_pair_again
import simpmusic.composeapp.generated.resources.library_sync_pair_message
import simpmusic.composeapp.generated.resources.library_sync_pair_title
import simpmusic.composeapp.generated.resources.library_sync_pairing
import simpmusic.composeapp.generated.resources.library_sync_revoked
import simpmusic.composeapp.generated.resources.library_sync_scan_hint
import simpmusic.composeapp.generated.resources.library_sync_settings_differ_android
import simpmusic.composeapp.generated.resources.library_sync_sync_now
import simpmusic.composeapp.generated.resources.library_sync_syncing
import simpmusic.composeapp.generated.resources.library_sync_syncs
import simpmusic.composeapp.generated.resources.library_sync_syncs_value
import simpmusic.composeapp.generated.resources.library_sync_title
import simpmusic.composeapp.generated.resources.library_sync_unpair
import simpmusic.composeapp.generated.resources.library_sync_unpair_message
import simpmusic.composeapp.generated.resources.library_sync_unpair_title
import simpmusic.composeapp.generated.resources.library_sync_unreachable
import simpmusic.composeapp.generated.resources.login_sync_allow_camera
import simpmusic.composeapp.generated.resources.login_sync_camera_blocked
import simpmusic.composeapp.generated.resources.login_sync_camera_needed
import simpmusic.composeapp.generated.resources.login_sync_connecting
import simpmusic.composeapp.generated.resources.login_sync_no_camera
import simpmusic.composeapp.generated.resources.login_sync_open_settings
import simpmusic.composeapp.generated.resources.retry

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun LibrarySyncDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val viewModel: LibrarySyncPhoneViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val step by viewModel.step.collectAsStateWithLifecycle()
    val pending by viewModel.pendingSettings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = rememberSurfaceDarkColors()
    var confirmUnpair by remember { mutableStateOf(false) }

    // Camera, plus Nearby devices on Android 17+, asked together as Login Sync does.
    val permissions = remember { loginSyncPermissions() }

    fun hasPermissions() = permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.isNotEmpty() && result.values.all { it }
            val blocked = !granted && !context.findActivity().shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            viewModel.onPermission(granted, blocked)
        }
    // Paired and only Nearby devices missing: ask for it alone, then run the round it was blocking.
    val nearbyLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.syncNow() else context.openAppSettings()
        }

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    LaunchedEffect(viewModel) { viewModel.open(hasPermissions()) }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    LifecycleResumeEffect(step) {
        if (step is Step.NeedsPermission && hasPermissions()) viewModel.onPermission(granted = true, blocked = false)
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
                        text = stringResource(Res.string.library_sync_title),
                        style = typo().titleMedium,
                        color = colors.content,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = ::close) {
                        Icon(imageVector = SimpIcons.Close, contentDescription = stringResource(Res.string.cancel), tint = colors.content)
                    }
                }
                if (state.isPaired) {
                    PairedStatus(
                        state = state,
                        onAllowNearby = { nearbyLauncher.launch(ACCESS_LOCAL_NETWORK) },
                        onSyncNow = viewModel::syncNow,
                        onUnpair = { confirmUnpair = true },
                    )
                    pending?.let {
                        Box(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                            PendingSettingsCard(
                                pending = it,
                                differ = stringResource(Res.string.library_sync_settings_differ_android),
                                onNotNow = viewModel::dismissSettings,
                                onApply = { viewModel.applySettings(::restartAfterSyncedSettings) },
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.library_sync_scan_hint),
                        style = typo().bodySmall,
                        color = colors.subtitle,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier =
                            Modifier
                                .padding(horizontal = 24.dp)
                                .widthIn(max = 340.dp)
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (step == Step.Scanning) Color.Black else colors.disabled.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (val s = step) {
                            Step.Scanning -> {
                                QrScanner(
                                    onResult = viewModel::onScanned,
                                    onCameraUnavailable = viewModel::onCameraUnavailable,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                ViewfinderCorners(Modifier.fillMaxSize().padding(20.dp))
                            }

                            Step.Reaching -> {
                                Status(stringResource(Res.string.login_sync_connecting), progress = true)
                            }

                            Step.Pairing -> {
                                Status(stringResource(Res.string.library_sync_pairing), progress = true)
                            }

                            is Step.Confirm -> {
                                Status("${s.desktop.name} · ${s.desktop.os}", progress = false)
                            }

                            is Step.NeedsPermission -> {
                                if (s.blocked) {
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

                            Step.Revoked -> {
                                Status(stringResource(Res.string.library_sync_revoked), progress = false) {
                                    SheetButton(stringResource(Res.string.library_sync_pair_again)) { viewModel.scan(hasPermissions()) }
                                }
                            }

                            is Step.Failed -> {
                                Status(problemText(s.problem, s.detail, null), progress = false) {
                                    SheetButton(stringResource(Res.string.retry)) { viewModel.scan(hasPermissions()) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    (step as? Step.Confirm)?.let { confirm ->
        if (!state.isPaired) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelConfirm(hasPermissions()) },
                title = { Text(text = stringResource(Res.string.library_sync_pair_title), style = typo().titleSmall) },
                text = { Text(text = stringResource(Res.string.library_sync_pair_message, confirm.desktop.name, confirm.desktop.os)) },
                confirmButton = {
                    TextButton(onClick = viewModel::pair) {
                        Text(text = stringResource(Res.string.library_sync_pair), color = LocalContentColor.current)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelConfirm(hasPermissions()) }) {
                        Text(text = stringResource(Res.string.cancel), color = LocalContentColor.current)
                    }
                },
            )
        }
    }

    if (confirmUnpair) {
        val name = state.peers.firstOrNull()?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { confirmUnpair = false },
            title = { Text(text = stringResource(Res.string.library_sync_unpair_title, name), style = typo().titleSmall) },
            text = { Text(text = stringResource(Res.string.library_sync_unpair_message, name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnpair = false
                    viewModel.unpair(hasPermissions())
                }) { Text(text = stringResource(Res.string.library_sync_unpair), color = LocalContentColor.current) }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnpair = false }) { Text(text = stringResource(Res.string.cancel), color = LocalContentColor.current) }
            },
        )
    }
}

/** A4/A5: the computer, what syncs, when it last did, and what stopped it. */
@Composable
private fun PairedStatus(
    state: LibrarySyncState,
    onAllowNearby: () -> Unit,
    onSyncNow: () -> Unit,
    onUnpair: () -> Unit,
) {
    val colors = rememberSurfaceDarkColors()
    val peer = state.peers.first()
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.problem?.let { problem ->
            if (problem == LibrarySyncProblem.NEEDS_LOCAL_NETWORK_PERMISSION) {
                SyncProblemBox(stringResource(Res.string.library_sync_needs_nearby), stringResource(Res.string.library_sync_allow), onAllowNearby)
            } else {
                SyncProblemBox(problemText(problem, state.problemDetail, peer.name))
            }
        }
        Column {
            Text(text = stringResource(Res.string.library_sync_computer), style = typo().bodySmall, color = colors.subtitle)
            Text(text = "${peer.name} · ${peer.os}", style = typo().bodyMedium, color = colors.content)
        }
        Column {
            Text(text = stringResource(Res.string.library_sync_syncs), style = typo().bodySmall, color = colors.subtitle)
            Text(text = stringResource(Res.string.library_sync_syncs_value), style = typo().bodyMedium, color = colors.content)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.syncing) CircularProgressIndicator(Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
            Text(
                text =
                    when {
                        state.syncing && state.lastSyncAt == null -> stringResource(Res.string.library_sync_first_sync_phone)
                        state.syncing -> stringResource(Res.string.library_sync_syncing)
                        else -> state.lastSyncAt?.let { stringResource(Res.string.library_sync_last_synced, syncedAgo(it)) }.orEmpty()
                    },
                style = typo().bodySmall,
                color = colors.subtitle,
            )
        }
        SheetButton(stringResource(Res.string.library_sync_sync_now), onSyncNow)
        TextButton(onClick = onUnpair, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(text = stringResource(Res.string.library_sync_unpair), color = colors.content)
        }
    }
}

@Composable
private fun problemText(
    problem: LibrarySyncProblem,
    detail: String?,
    desktopName: String?,
): String =
    when (problem) {
        LibrarySyncProblem.UNREACHABLE -> stringResource(Res.string.library_sync_unreachable, desktopName ?: "your computer")
        LibrarySyncProblem.NEEDS_LOCAL_NETWORK_PERMISSION -> stringResource(Res.string.library_sync_needs_nearby)
        LibrarySyncProblem.REVOKED -> stringResource(Res.string.library_sync_revoked)
        else -> stringResource(Res.string.library_sync_failed, detail.orEmpty())
    }

actual fun restartAfterSyncedSettings() {
    val context: android.content.Context = getKoin().get()
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
    context.startActivity(Intent.makeRestartActivityTask(launch?.component))
    Runtime.getRuntime().exit(0)
}
