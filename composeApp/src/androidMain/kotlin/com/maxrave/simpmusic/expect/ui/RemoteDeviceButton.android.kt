package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maxrave.media3.heos.DenonReceiverState
import com.maxrave.media3.heos.DenonSoundMode
import com.maxrave.media3.remote.RemoteDevice
import com.maxrave.media3.remote.RemoteDeviceManager
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Speaker
import com.maxrave.simpmusic.ui.icon.VolumeOff
import com.maxrave.simpmusic.ui.icon.VolumeUp
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.getKoin
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.heos_connect
import simpmusic.composeapp.generated.resources.heos_manual_ip
import simpmusic.composeapp.generated.resources.heos_muted
import simpmusic.composeapp.generated.resources.heos_no_devices
import simpmusic.composeapp.generated.resources.heos_now_playing_on
import simpmusic.composeapp.generated.resources.heos_play_on_device
import simpmusic.composeapp.generated.resources.heos_power_on
import simpmusic.composeapp.generated.resources.heos_receiver_controls
import simpmusic.composeapp.generated.resources.heos_search_again
import simpmusic.composeapp.generated.resources.heos_searching
import simpmusic.composeapp.generated.resources.heos_sound_mode
import simpmusic.composeapp.generated.resources.heos_stop_remote
import simpmusic.composeapp.generated.resources.heos_subtitle
import simpmusic.composeapp.generated.resources.heos_volume

/**
 * Speaker icon that opens the device picker (HEOS + DLNA). [RemoteDeviceManager] is registered by the media
 * service module; if that module is not loaded yet (no playback has started) nothing renders.
 */
@Composable
actual fun PlatformRemoteDeviceButton(
    modifier: Modifier,
    tint: Color,
) {
    val koin = getKoin()
    val manager = remember(koin) { koin.getOrNull<RemoteDeviceManager>() } ?: return
    var showSheet by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }

    Icon(
        imageVector = SimpIcons.Speaker,
        contentDescription = stringResource(Res.string.heos_play_on_device),
        tint = tint,
        modifier =
            modifier.clickable(
                interactionSource = interaction,
                indication = null,
                onClick = { showSheet = true },
            ),
    )

    if (showSheet) {
        HeosDevicePickerSheet(manager = manager, onDismiss = { showSheet = false })
    }
}

actual fun isPlatformRemoteDeviceAvailable(): Boolean = true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeosDevicePickerSheet(
    manager: RemoteDeviceManager,
    onDismiss: () -> Unit,
) {
    val devices by manager.devices.collectAsState()
    val searching by manager.isSearching.collectAsState()
    val connected by manager.connectedDevice.collectAsState()
    val receiver by manager.receiver.collectAsState()
    var manualIp by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val colorScheme = MaterialTheme.colorScheme
    // Same surface + type the app's other sheets use, so this one does not look bolted on.
    val surface = rememberSurfaceDarkColors()
    val typography = typo()

    LaunchedEffect(manager) {
        error = null
        manager.discover()
    }
    LaunchedEffect(manager) {
        manager.errors.collect { error = it }
    }
    // Close the CLI socket when the picker goes away without a device in use.
    DisposableEffect(manager) {
        onDispose { manager.releaseIfIdle() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = surface.container,
        contentColor = surface.content,
        scrimColor = Color.Black.copy(alpha = .5f),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(Res.string.heos_play_on_device),
                style = typography.titleLarge,
                color = surface.content,
            )
            Text(
                text = stringResource(Res.string.heos_subtitle),
                style = typography.bodyMedium,
                color = surface.subtitle,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(12.dp))

            connected?.let { device ->
                Text(
                    text = stringResource(Res.string.heos_now_playing_on, device.name),
                    style = typography.bodyMedium,
                    color = colorScheme.primary,
                )
                OutlinedButton(
                    onClick = {
                        manager.disconnect()
                        onDismiss()
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(Res.string.heos_stop_remote), color = surface.content)
                }
                receiver?.let { avr ->
                    ReceiverControls(avr = avr, manager = manager)
                }
                Spacer(Modifier.height(12.dp))
            }

            when {
                searching && devices.isEmpty() -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(Res.string.heos_searching),
                            style = typography.bodyMedium,
                            color = surface.subtitle,
                        )
                    }
                }
                devices.isEmpty() -> {
                    Text(
                        text = stringResource(Res.string.heos_no_devices),
                        style = typography.bodyMedium,
                        color = surface.subtitle,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }

            devices.forEach { device ->
                HeosDeviceRow(
                    device = device,
                    isActive = connected?.id == device.id,
                    onClick = {
                        manager.connect(device)
                        onDismiss()
                    },
                )
            }

            error?.let {
                Text(
                    text = it,
                    style = typography.bodySmall,
                    color = colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = manualIp,
                    onValueChange = { manualIp = it },
                    label = { Text(stringResource(Res.string.heos_manual_ip)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    enabled = manualIp.isNotBlank() && !searching,
                    onClick = {
                        error = null
                        manager.discover(manualHost = manualIp)
                    },
                ) {
                    Text(stringResource(Res.string.heos_connect))
                }
            }
            TextButton(
                enabled = !searching,
                onClick = {
                    error = null
                    manager.discover()
                },
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(stringResource(Res.string.heos_search_again))
            }
        }
    }
}

/**
 * Denon / Marantz receiver controls (port 23): surround program, master volume in dB, mute,
 * power. Only shown while the manager has a live control link, so HEOS speakers never see it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReceiverControls(
    avr: DenonReceiverState,
    manager: RemoteDeviceManager,
) {
    val colorScheme = MaterialTheme.colorScheme
    val surface = rememberSurfaceDarkColors()
    val typography = typo()
    // Slider position while the finger is down; the receiver's own value takes over on release.
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shownDb = dragging ?: avr.volumeDb
    val minDb = DenonReceiverState.MIN_VOLUME_DB
    val maxDb = avr.maxVolumeDb.coerceAtLeast(minDb + 1f)

    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.heos_receiver_controls),
                style = typography.titleSmall,
                color = surface.content,
            )
            Spacer(Modifier.weight(1f))
            if (avr.poweredOn == false) {
                TextButton(onClick = { manager.receiverPowerOn() }) {
                    Text(stringResource(Res.string.heos_power_on))
                }
            }
        }

        // Sound program — the receiver's upmixer is what spreads stereo across every speaker.
        Text(
            text =
                buildString {
                    append(stringResource(Res.string.heos_sound_mode))
                    avr.soundModeName?.let { append(" · ").append(it) }
                },
            style = typography.bodySmall,
            color = surface.subtitle,
            modifier = Modifier.padding(top = 8.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            DenonSoundMode.entries.forEach { mode ->
                FilterChip(
                    selected = avr.soundMode == mode,
                    onClick = { manager.setSoundMode(mode) },
                    label = { Text(mode.label) },
                )
            }
        }

        // Master volume in the receiver's own dB scale.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(
                text = stringResource(Res.string.heos_volume),
                style = typography.bodySmall,
                color = surface.subtitle,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text =
                    when {
                        avr.muted -> stringResource(Res.string.heos_muted)
                        shownDb != null -> "%.1f dB".format(shownDb)
                        else -> "—"
                    },
                style = typography.bodyMedium,
                color = if (avr.muted) colorScheme.error else surface.content,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { manager.setReceiverMuted(!avr.muted) }) {
                Icon(
                    imageVector = if (avr.muted) SimpIcons.VolumeOff else SimpIcons.VolumeUp,
                    contentDescription = stringResource(Res.string.heos_muted),
                    tint = if (avr.muted) colorScheme.error else surface.subtitle,
                )
            }
            Slider(
                value = (shownDb ?: minDb).coerceIn(minDb, maxDb),
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    // Continuous slider (200 tick marks would be noise); snap to the receiver's half-dB steps on release.
                    dragging?.let { manager.setReceiverVolumeDb(kotlin.math.round(it * 2f) / 2f) }
                    dragging = null
                },
                valueRange = minDb..maxDb,
                enabled = avr.volumeDb != null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeosDeviceRow(
    device: RemoteDevice,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val surface = rememberSurfaceDarkColors()
    ListItem(
        headlineContent = { Text(device.name, style = typo().bodyLarge, color = surface.content) },
        supportingContent = {
            Text(
                listOf(device.kind.label, device.model.takeIf { it.isNotBlank() }, device.ip).filterNotNull().joinToString(" · "),
                style = typo().bodySmall,
                color = surface.subtitle,
            )
        },
        leadingContent = {
            Icon(
                imageVector = SimpIcons.Speaker,
                contentDescription = null,
                tint = if (isActive) colorScheme.primary else surface.subtitle,
            )
        },
        trailingContent = {
            if (isActive) {
                Icon(
                    imageVector = SimpIcons.Check,
                    contentDescription = null,
                    tint = colorScheme.primary,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = !isActive, onClick = onClick),
    )
}
