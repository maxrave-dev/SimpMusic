package com.maxrave.simpmusic.expect.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.extension.findActivity
import com.maxrave.simpmusic.recognition.Recognition
import com.maxrave.simpmusic.recognition.SongRecognizer
import com.maxrave.simpmusic.ui.component.SurfaceDarkColors
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.CloudOff
import com.maxrave.simpmusic.ui.icon.GraphicEq
import com.maxrave.simpmusic.ui.icon.MicOff
import com.maxrave.simpmusic.ui.icon.MusicNote
import com.maxrave.simpmusic.ui.icon.MusicOff
import com.maxrave.simpmusic.ui.icon.Refresh
import com.maxrave.simpmusic.ui.icon.Replay
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.login_sync_open_settings
import simpmusic.composeapp.generated.resources.recognize_allow_mic
import simpmusic.composeapp.generated.resources.recognize_failed
import simpmusic.composeapp.generated.resources.recognize_failed_hint
import simpmusic.composeapp.generated.resources.recognize_find
import simpmusic.composeapp.generated.resources.recognize_identifying
import simpmusic.composeapp.generated.resources.recognize_listen_again
import simpmusic.composeapp.generated.resources.recognize_listening
import simpmusic.composeapp.generated.resources.recognize_listening_hint
import simpmusic.composeapp.generated.resources.recognize_mic_hint
import simpmusic.composeapp.generated.resources.recognize_mic_needed
import simpmusic.composeapp.generated.resources.recognize_no_match
import simpmusic.composeapp.generated.resources.recognize_no_match_hint
import simpmusic.composeapp.generated.resources.recognize_song
import simpmusic.composeapp.generated.resources.recognize_try_again

private val OrbSize = 236.dp
private val CoreSize = 118.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun SongRecognitionSheet(
    onFind: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val localDensity = LocalDensity.current
    val colors = rememberSurfaceDarkColors()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun hasMic() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    var micGranted by remember { mutableStateOf(hasMic()) }
    var micBlocked by remember { mutableStateOf(false) }
    // Bumped by Try again and Listen again; each value is one pass.
    var attempt by remember { mutableIntStateOf(0) }
    var recognition by remember { mutableStateOf<Recognition>(Recognition.Listening) }

    val micLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            micGranted = granted
            // Rationale is false after a denial only when the system will not ask again.
            micBlocked = !granted && !context.findActivity().shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
        }

    // Back from the system settings page with the microphone now allowed.
    LifecycleResumeEffect(micGranted) {
        if (!micGranted && hasMic()) micGranted = true
        onPauseOrDispose { }
    }

    // ponytail: the pass lives in this composition, so closing the sheet stops the recording and a
    // rotation starts listening again; move it to a ViewModel if a result must survive rotation.
    LaunchedEffect(micGranted, attempt) {
        if (!micGranted) return@LaunchedEffect
        recognition = Recognition.Listening
        recognition = SongRecognizer.recognize(onIdentifying = { recognition = Recognition.Identifying })
    }

    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            then()
            onDismiss()
        }
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
        // Transparent above the card, so the sheet stops below the status bar.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() }),
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = colors.container),
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(4.dp),
                    colors = CardDefaults.cardColors().copy(containerColor = colors.handle),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.recognize_song),
                        style = typo().titleMedium,
                        color = colors.content,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { close() }) {
                        Icon(imageVector = SimpIcons.Close, contentDescription = null, tint = colors.content)
                    }
                }
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 28.dp, end = 28.dp, bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                ) {
                    val state = recognition
                    when {
                        !micGranted -> {
                            RecognitionOrb(SimpIcons.MicOff, muted = true)
                            Message(stringResource(Res.string.recognize_mic_needed), stringResource(Res.string.recognize_mic_hint), colors)
                            if (micBlocked) {
                                SheetButton(stringResource(Res.string.login_sync_open_settings)) { context.openAppSettings() }
                            } else {
                                SheetButton(stringResource(Res.string.recognize_allow_mic)) {
                                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }

                        state is Recognition.Found -> {
                            AsyncImage(
                                model = state.coverArt,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .size(OrbSize)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.handle),
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = state.title,
                                    style = typo().titleMedium,
                                    color = colors.content,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = state.artist,
                                    style = typo().bodyMedium,
                                    color = colors.subtitle,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Column(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SheetButton(stringResource(Res.string.recognize_find), SimpIcons.Search) {
                                    val query = "${state.title} ${state.artist}".trim()
                                    close { onFind(query) }
                                }
                                TextButton(
                                    onClick = { attempt++ },
                                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                                ) {
                                    Icon(imageVector = SimpIcons.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = stringResource(Res.string.recognize_listen_again), color = LocalContentColor.current)
                                }
                            }
                        }

                        state is Recognition.NoMatch || state is Recognition.Failed -> {
                            val noMatch = state is Recognition.NoMatch
                            RecognitionOrb(if (noMatch) SimpIcons.MusicOff else SimpIcons.CloudOff, muted = true)
                            Message(
                                title = stringResource(if (noMatch) Res.string.recognize_no_match else Res.string.recognize_failed),
                                hint = stringResource(if (noMatch) Res.string.recognize_no_match_hint else Res.string.recognize_failed_hint),
                                colors = colors,
                            )
                            SheetButton(stringResource(Res.string.recognize_try_again), SimpIcons.Refresh) { attempt++ }
                        }

                        else -> {
                            val listening = state is Recognition.Listening
                            RecognitionOrb(if (listening) SimpIcons.GraphicEq else SimpIcons.MusicNote, listening = listening)
                            Message(
                                title = stringResource(if (listening) Res.string.recognize_listening else Res.string.recognize_identifying),
                                // Kept as an empty line while identifying, so the layout does not jump.
                                hint = if (listening) stringResource(Res.string.recognize_listening_hint) else "",
                                colors = colors,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The disc in the middle of the sheet; while listening, rings spread out from it. */
@Composable
private fun RecognitionOrb(
    icon: ImageVector,
    muted: Boolean = false,
    listening: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.size(if (muted) 140.dp else OrbSize), contentAlignment = Alignment.Center) {
        if (listening) {
            val ring = cs.primary
            val progress by rememberInfiniteTransition(label = "rings").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
                label = "ring",
            )
            Canvas(Modifier.size(OrbSize)) {
                val stroke = 2.dp.toPx()
                val from = CoreSize.toPx() / 2
                val to = size.minDimension / 2 - stroke / 2
                // Two rings half a cycle apart, fading as they grow.
                for (k in 0..1) {
                    val p = (progress + k / 2f) % 1f
                    drawCircle(ring, radius = from + (to - from) * p, alpha = 0.5f * (1f - p), style = Stroke(stroke))
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .size(CoreSize)
                    .clip(CircleShape)
                    .background(if (muted) cs.surfaceContainerHighest else cs.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (muted) cs.onSurfaceVariant else cs.onPrimary,
                modifier = Modifier.size(52.dp),
            )
        }
    }
}

@Composable
private fun Message(
    title: String,
    hint: String,
    colors: SurfaceDarkColors,
) {
    Text(text = title, style = typo().titleMedium, color = colors.content, textAlign = TextAlign.Center)
    Text(text = hint, style = typo().bodyMedium, color = colors.subtitle, textAlign = TextAlign.Center)
}

@Composable
private fun SheetButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        contentPadding = if (icon != null) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, color = LocalContentColor.current)
    }
}
