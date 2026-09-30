package com.maxrave.simpmusic.ui.screen.player.content.applemusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maxrave.domain.data.player.AudioOutput
import com.maxrave.domain.data.player.AudioOutputKind
import com.maxrave.domain.data.player.GenericCastState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.DeviceVolumeController
import com.maxrave.simpmusic.expect.ui.rememberCastReceivers
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.SurfaceDarkColors
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.Headphones
import com.maxrave.simpmusic.ui.icon.MusicCast
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Smartphone
import com.maxrave.simpmusic.ui.icon.Speaker
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.audio_output
import simpmusic.composeapp.generated.resources.audio_output_bluetooth
import simpmusic.composeapp.generated.resources.audio_output_cast
import simpmusic.composeapp.generated.resources.audio_output_hdmi
import simpmusic.composeapp.generated.resources.audio_output_playing_here
import simpmusic.composeapp.generated.resources.audio_output_system_default
import simpmusic.composeapp.generated.resources.audio_output_usb
import simpmusic.composeapp.generated.resources.audio_output_wired

/**
 * Where the sound goes — opened from the headphones half of the action row's capsule.
 *
 * The phone's own outputs and the Cast receivers are one list, the way Apple lists AirPlay speakers
 * beside the iPhone: they answer the same question, so they belong in the same place. Picking a
 * local output while casting ends the Cast session first. Built in the app's own sheet style (the
 * same card, handle and rows as the Now Playing ⋯ sheet), not a new one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppleMusicOutputSheet(
    outputs: List<AudioOutput>,
    castState: GenericCastState,
    deviceVolumeController: DeviceVolumeController?,
    onSelectOutput: (AudioOutput) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = rememberSurfaceDarkColors()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val localDensity = LocalDensity.current
    // Scanning for receivers only while this is open: discovery keeps the radio busy.
    val cast = rememberCastReceivers(discover = true)
    // Devices coming and going update the list on their own; the system's own route changing does not.
    LaunchedEffect(Unit) { onRefresh() }

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
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = colors.container),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier =
                        Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.handle),
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = stringResource(Res.string.audio_output),
                    style = typo().labelMedium,
                    color = colors.content,
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
                )
                outputs.forEach { output ->
                    val playingHere = output.isActive && !castState.isRemote
                    OutputRow(
                        icon = output.icon(),
                        name = output.displayName(),
                        status = if (playingHere) stringResource(Res.string.audio_output_playing_here) else output.kindLabel(),
                        selected = playingHere,
                        colors = colors,
                        onClick = {
                            // Coming back from a receiver: end the Cast session first, or the pick
                            // would route a local player that is not the one playing.
                            if (castState.isRemote) cast.disconnect()
                            onSelectOutput(output)
                        },
                    )
                }
                if (cast.receivers.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.10f),
                    )
                    Text(
                        text = stringResource(Res.string.audio_output_cast),
                        style = typo().bodySmall,
                        color = colors.subtitle,
                        modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 20.dp, top = 4.dp),
                    )
                    cast.receivers.forEach { receiver ->
                        OutputRow(
                            icon = SimpIcons.MusicCast,
                            name = receiver.name,
                            status = if (receiver.isConnected) stringResource(Res.string.audio_output_playing_here) else null,
                            selected = receiver.isConnected,
                            colors = colors,
                            onClick = { if (!receiver.isConnected) cast.connect(receiver.id) },
                        )
                    }
                }
                deviceVolumeController?.let { controller ->
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.10f),
                    )
                    AppleMusicVolumeRow(controller = controller, modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp))
                }
                Spacer(
                    modifier =
                        Modifier.height(
                            with(localDensity) { WindowInsets.systemBars.getBottom(localDensity).toDp() } + 12.dp,
                        ),
                )
            }
        }
    }
}

// One output: the ⋯ sheet's ActionButton metrics (48dp icon box, labelSmall), with a status line
// under the name and a tick on the one playing.
@Composable
private fun OutputRow(
    icon: ImageVector,
    name: String,
    status: String?,
    selected: Boolean,
    colors: SurfaceDarkColors,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.content, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(text = name, style = typo().labelSmall, color = colors.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (status != null) {
                Text(text = status, style = typo().bodySmall, color = colors.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        AnimatedVisibility(visible = selected, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            Icon(imageVector = SimpIcons.Check, contentDescription = null, tint = colors.content, modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * What to call an output. A backend leaves the name blank when all it has is meaningless — the
 * desktop's "auto", or a wire reporting the phone's own model — and the kind names it instead.
 */
@Composable
internal fun AudioOutput.displayName(): String =
    name.ifBlank {
        when (kind) {
            AudioOutputKind.DEVICE_SPEAKER -> stringResource(Res.string.audio_output_system_default)
            AudioOutputKind.WIRED -> stringResource(Res.string.audio_output_wired)
            AudioOutputKind.BLUETOOTH -> stringResource(Res.string.audio_output_bluetooth)
            AudioOutputKind.USB -> stringResource(Res.string.audio_output_usb)
            AudioOutputKind.HDMI -> stringResource(Res.string.audio_output_hdmi)
            AudioOutputKind.OTHER -> stringResource(Res.string.audio_output_system_default)
        }
    }

// The second line under a device that is not playing: what kind of connection it is, when the name
// alone does not say so.
@Composable
private fun AudioOutput.kindLabel(): String? =
    when (kind) {
        AudioOutputKind.BLUETOOTH -> stringResource(Res.string.audio_output_bluetooth)
        AudioOutputKind.USB -> if (name.isNotBlank()) stringResource(Res.string.audio_output_usb) else null
        AudioOutputKind.HDMI -> if (name.isNotBlank()) stringResource(Res.string.audio_output_hdmi) else null
        else -> null
    }

private fun AudioOutput.icon(): ImageVector =
    when (kind) {
        // The phone's own speaker on Android; the system default on desktop, which is a computer.
        AudioOutputKind.DEVICE_SPEAKER -> if (getPlatform() == Platform.Android) SimpIcons.Smartphone else SimpIcons.Speaker
        AudioOutputKind.WIRED, AudioOutputKind.BLUETOOTH, AudioOutputKind.USB -> SimpIcons.Headphones
        AudioOutputKind.HDMI, AudioOutputKind.OTHER -> SimpIcons.Speaker
    }
