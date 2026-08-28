package com.maxrave.simpmusic.ui.component.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.saveImageToDevice
import com.maxrave.simpmusic.expect.shareImage
import com.maxrave.simpmusic.expect.ui.rememberSaveImagePermission
import com.maxrave.simpmusic.expect.ui.toPngByteArray
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.capture.capturable
import com.maxrave.simpmusic.ui.component.capture.rememberCaptureController
import com.maxrave.simpmusic.ui.icon.Download
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.appleMusicVerticalFadeEdges
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.share_lyrics
import simpmusic.composeapp.generated.resources.share_lyrics_background
import simpmusic.composeapp.generated.resources.share_lyrics_continue
import simpmusic.composeapp.generated.resources.share_lyrics_max_reached
import simpmusic.composeapp.generated.resources.share_lyrics_permission_denied
import simpmusic.composeapp.generated.resources.share_lyrics_save
import simpmusic.composeapp.generated.resources.share_lyrics_save_failed
import simpmusic.composeapp.generated.resources.share_lyrics_saved
import simpmusic.composeapp.generated.resources.share_lyrics_saved_desktop
import simpmusic.composeapp.generated.resources.share_lyrics_select_title
import simpmusic.composeapp.generated.resources.share_lyrics_selected_count
import simpmusic.composeapp.generated.resources.share_lyrics_share_action
import simpmusic.composeapp.generated.resources.share_lyrics_share_failed
import kotlin.random.Random

/**
 * Pick up to [MAX_SHARE_LYRIC_LINES] consecutive lines, preview the card they make, then save or
 * share it.
 *
 * The surface is tinted from the artwork rather than from the app theme, which is what both
 * Spotify and YouTube Music do here and the reason their version of this screen looks like it
 * belongs to the song. A fixed grey panel reads as a system dialog that wandered in — and it also
 * ignores light mode and dark mode equally, which is the worst of both. Everything drawn on top
 * takes its colour from that tint via [shareCardContentColor], so contrast follows the artwork
 * instead of being hardcoded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLyricsSheet(
    lines: List<String>,
    songTitle: String,
    artistName: String,
    artwork: ImageBitmap?,
    seedColor: Color,
    initialLineIndex: Int,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Starts on the line being sung, which is the whole reason this opens as a list at all — the
    // user already had that line in front of them when they reached for share.
    val selection = remember { ShareLyricsSelection(initialLineIndex.takeIf { it in lines.indices }) }
    var showPreview by remember { mutableStateOf(false) }
    var cardBackground by remember(seedColor) { mutableStateOf(seedColor) }
    var busy by remember { mutableStateOf(false) }

    val captureController = rememberCaptureController()

    // Both reference apps ramp the artwork tint down towards black rather than filling flat: the
    // list scrolls under it, and a flat fill makes the lines at the bottom fight the background.
    val surfaceBrush =
        remember(seedColor) {
            Brush.verticalGradient(listOf(seedColor, lerp(seedColor, Color.Black, 0.82f)))
        }
    val content = seedColor.shareCardContentColor()
    // The colour anything printed ON a filled surface uses. Computed once against `content`, which
    // is what those surfaces are actually filled with — never the raw seed.
    val onFilled = seedColor.shareTintOn(content)

    val limitMessage = stringResource(Res.string.share_lyrics_max_reached, MAX_SHARE_LYRIC_LINES)
    val savedMessage =
        stringResource(
            if (getPlatform() == Platform.Desktop) Res.string.share_lyrics_saved_desktop else Res.string.share_lyrics_saved,
        )
    val saveFailedMessage = stringResource(Res.string.share_lyrics_save_failed)
    val shareFailedMessage = stringResource(Res.string.share_lyrics_share_failed)
    val permissionDeniedMessage = stringResource(Res.string.share_lyrics_permission_denied)
    val chooserTitle = stringResource(Res.string.share_lyrics)

    val fileName =
        remember(songTitle) {
            val stem = songTitle.ifBlank { "lyrics" }.take(32).map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
            "SimpMusic_${stem}_${Random.nextInt(100_000, 999_999)}.png"
        }

    // Saving is the only half that can be refused: sharing goes through the app's own cache, which
    // needs nothing. On Android 10 and up, and on Desktop, this grants without a dialog.
    val savePermission =
        rememberSaveImagePermission { granted ->
            if (!granted) {
                showToast(permissionDeniedMessage, ToastGravity.Bottom)
                busy = false
                return@rememberSaveImagePermission
            }
            scope.launch {
                val bytes = captureController.captureAsync().await().toPngByteArray()
                val ok = bytes != null && saveImageToDevice(bytes, fileName)
                showToast(if (ok) savedMessage else saveFailedMessage, ToastGravity.Bottom)
                busy = false
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The gradient is painted inside; a container colour here would sit under it as a seam.
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .6f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (showPreview) Modifier else Modifier.fillMaxHeight(0.94f))
                    .background(surfaceBrush),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                ShareLyricsSheetHeader(
                    title =
                        if (showPreview) {
                            stringResource(Res.string.share_lyrics)
                        } else {
                            stringResource(Res.string.share_lyrics_select_title)
                        },
                    subtitle =
                        if (showPreview) null else stringResource(Res.string.share_lyrics_selected_count, selection.count),
                    content = content,
                    onClose = { if (showPreview) showPreview = false else onDismiss() },
                )

                if (showPreview) {
                    ShareLyricsPreview(
                        selection = selection,
                        lines = lines,
                        songTitle = songTitle,
                        artistName = artistName,
                        artwork = artwork,
                        seedColor = seedColor,
                        cardBackground = cardBackground,
                        onSelectBackground = { cardBackground = it },
                        content = content,
                        onFilled = onFilled,
                        captureModifier = Modifier.capturable(captureController),
                        onSave = {
                            if (!busy) {
                                busy = true
                                // The save itself lives in the permission callback, which always
                                // fires exactly once — so `busy` is cleared on every path.
                                savePermission.requestIfNeeded()
                            }
                        },
                        onShare = {
                            if (!busy) {
                                busy = true
                                scope.launch {
                                    val bytes = captureController.captureAsync().await().toPngByteArray()
                                    val ok = bytes != null && shareImage(bytes, fileName, chooserTitle)
                                    if (!ok) showToast(shareFailedMessage, ToastGravity.Bottom)
                                    busy = false
                                }
                            }
                        },
                    )
                } else {
                    ShareLyricsPicker(
                        lines = lines,
                        selection = selection,
                        onFilled = onFilled,
                        content = content,
                        initialLineIndex = initialLineIndex,
                        onLimitReached = { showToast(limitMessage, ToastGravity.Bottom) },
                        onContinue = { showPreview = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Chevron on the left and the title centred, the way Spotify lays this out — the count rides
 * under the title as a sentence rather than as a "3 / 3" fraction, which reads like a form field.
 */
@Composable
private fun ShareLyricsSheetHeader(
    title: String,
    subtitle: String?,
    content: Color,
    onClose: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SimpIcons.KeyboardArrowDown,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(26.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, color = content, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, color = content.copy(alpha = 0.62f), fontSize = 12.sp)
            }
        }
        // Balances the chevron so the title sits on the true centre.
        Spacer(modifier = Modifier.size(44.dp))
    }
}

/**
 * The line list.
 *
 * A picked line inverts — it is filled with the content colour and prints in the surface tint,
 * which is the strongest contrast available without introducing a colour that is not already on
 * screen. That is what both reference apps do, and it is why neither needs a checkmark: the fill
 * IS the state. An unpicked line simply steps back in alpha.
 */
@Composable
private fun ShareLyricsPicker(
    lines: List<String>,
    selection: ShareLyricsSelection,
    onFilled: Color,
    content: Color,
    initialLineIndex: Int,
    onLimitReached: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState =
        rememberLazyListState(
            initialFirstVisibleItemIndex = (initialLineIndex - 1).coerceIn(0, maxOf(0, lines.lastIndex)),
        )

    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().appleMusicVerticalFadeEdges(topFade = 24.dp, bottomFade = 96.dp),
            // Room for the floating pill to hover over the last lines rather than cover them.
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
        ) {
            itemsIndexed(lines) { index, text ->
                // The list is NOT compacted: an index here has to mean the same thing as
                // `initialLineIndex`, which counts blank separator lines like every other. They
                // render as a gap instead of an empty tappable row.
                if (text.isBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    ShareLyricsLineRow(
                        text = text,
                        selected = selection.isSelected(index),
                        onFilled = onFilled,
                        content = content,
                        onClick = { selection.toggle(index, onLimitReached) },
                    )
                }
            }
        }

        ShareLyricsPill(
            text = stringResource(Res.string.share_lyrics_continue),
            container = content,
            label = onFilled,
            enabled = !selection.isEmpty,
            onClick = onContinue,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun ShareLyricsLineRow(
    text: String,
    selected: Boolean,
    onFilled: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) content else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            // A filled row prints in the artwork's hue pushed away from the fill, so the pair
            // contrasts by construction however light or dark the artwork turned out.
            color = if (selected) onFilled else content.copy(alpha = 0.5f),
            fontSize = 18.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** The finished card, the background choices, and the two things you can do with it. */
@Composable
private fun ShareLyricsPreview(
    selection: ShareLyricsSelection,
    lines: List<String>,
    songTitle: String,
    artistName: String,
    artwork: ImageBitmap?,
    seedColor: Color,
    cardBackground: Color,
    onSelectBackground: (Color) -> Unit,
    content: Color,
    onFilled: Color,
    captureModifier: Modifier,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val selectedLines =
        remember(selection.range, lines) {
            // A pick can span a blank separator when it grows across a verse break; the card drops
            // it rather than printing an empty row.
            selection.range
                ?.mapNotNull { lines.getOrNull(it)?.takeIf(String::isNotBlank) }
                .orEmpty()
        }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        ShareLyricsCard(
            lines = selectedLines,
            songTitle = songTitle,
            artistName = artistName,
            artwork = artwork,
            background = cardBackground,
            modifier = captureModifier,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ShareLyricsPalette(
            seedColor = seedColor,
            selected = cardBackground,
            content = content,
            onSelect = onSelectBackground,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShareLyricsPill(
                text = stringResource(Res.string.share_lyrics_save),
                icon = SimpIcons.Download,
                container = Color.Transparent,
                label = content,
                outlined = true,
                onClick = onSave,
            )
            ShareLyricsPill(
                text = stringResource(Res.string.share_lyrics_share_action),
                icon = SimpIcons.Share,
                container = content,
                label = onFilled,
                onClick = onShare,
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

/**
 * The floating action both reference apps use: a wrap-width pill, not a full-bleed button.
 *
 * Full width reads as "this form is finished"; a pill reads as "carry on", which is what this step
 * actually is — and it lets the list keep scrolling visibly underneath it.
 */
@Composable
private fun ShareLyricsPill(
    text: String,
    container: Color,
    label: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    outlined: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(if (enabled) container else container.copy(alpha = 0.35f))
                .then(
                    if (outlined) Modifier.border(1.dp, label.copy(alpha = 0.45f), CircleShape) else Modifier,
                ).clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 28.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) label else label.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = if (enabled) label else label.copy(alpha = 0.5f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * The background choices.
 *
 * First swatch is the artwork's own dominant colour — the default, and the one both reference apps
 * open on, because a card tinted like the cover reads as belonging to the song. The rest are fixed
 * so there is always something to fall back on when the artwork is grey.
 */
@Composable
private fun ShareLyricsPalette(
    seedColor: Color,
    selected: Color,
    content: Color,
    onSelect: (Color) -> Unit,
) {
    val swatches =
        remember(seedColor) {
            listOf(
                seedColor,
                Color(0xFF1F1F1F),
                Color(0xFF2F5D50),
                Color(0xFF7A3B3B),
                Color(0xFFE8E2D4),
            )
        }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(Res.string.share_lyrics_background),
            color = content.copy(alpha = 0.6f),
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            swatches.forEach { swatch ->
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (swatch == selected) 2.dp else 0.dp,
                                color = if (swatch == selected) content else Color.Transparent,
                                shape = CircleShape,
                            ).clickable { onSelect(swatch) },
                )
            }
        }
    }
}
