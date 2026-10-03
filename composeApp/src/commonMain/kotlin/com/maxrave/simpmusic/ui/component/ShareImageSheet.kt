package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.saveImageToDevice
import com.maxrave.simpmusic.expect.shareImage
import com.maxrave.simpmusic.expect.ui.rememberSaveImagePermission
import com.maxrave.simpmusic.expect.ui.toPngByteArray
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.capture.capturable
import com.maxrave.simpmusic.ui.component.capture.rememberCaptureController
import com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsPill
import com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsSheetHeader
import com.maxrave.simpmusic.ui.component.lyrics.shareCardContentColor
import com.maxrave.simpmusic.ui.component.lyrics.shareTintOn
import com.maxrave.simpmusic.ui.icon.Download
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.share_lyrics_permission_denied
import simpmusic.composeapp.generated.resources.share_lyrics_save
import simpmusic.composeapp.generated.resources.share_lyrics_save_failed
import simpmusic.composeapp.generated.resources.share_lyrics_saved
import simpmusic.composeapp.generated.resources.share_lyrics_saved_desktop
import simpmusic.composeapp.generated.resources.share_lyrics_share_action
import simpmusic.composeapp.generated.resources.share_lyrics_share_failed
import kotlin.random.Random

/**
 * A bottom sheet that turns one composable into an image to save or send: the lyrics share sheet's
 * header and pills around a single [image], with the capture, the storage permission and the
 * failure toasts handled once here instead of in every sheet that shares a picture.
 *
 * [image] receives the modifier that marks what gets captured and must apply it to its root.
 * [belowImage] sits between the image and the pills, and is handed the sheet's content colour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareImageSheet(
    title: String,
    seedColor: Color,
    fileNamePrefix: String,
    onDismiss: () -> Unit,
    belowImage: (@Composable (content: Color) -> Unit)? = null,
    image: @Composable (captureModifier: Modifier) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    val captureController = rememberCaptureController()
    val surfaceBrush =
        remember(seedColor) {
            Brush.verticalGradient(listOf(seedColor, lerp(seedColor, Color.Black, 0.82f)))
        }
    val content = seedColor.shareCardContentColor()
    val onFilled = seedColor.shareTintOn(content)

    val savedMessage =
        stringResource(
            if (getPlatform() == Platform.Desktop) Res.string.share_lyrics_saved_desktop else Res.string.share_lyrics_saved,
        )
    val saveFailedMessage = stringResource(Res.string.share_lyrics_save_failed)
    val shareFailedMessage = stringResource(Res.string.share_lyrics_share_failed)
    val permissionDeniedMessage = stringResource(Res.string.share_lyrics_permission_denied)
    val fileName = remember { "${fileNamePrefix}_${Random.nextInt(100_000, 999_999)}.png" }

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
        // Scrolls because the image can be taller than a short phone: a long taste reading or a
        // receipt alone can fill the sheet, and the pills would otherwise be pushed off screen.
        // The gradient sits outside the scroll so it stays put while the content moves.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(surfaceBrush)
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            ShareLyricsSheetHeader(
                title = title,
                subtitle = null,
                content = content,
                onClose = onDismiss,
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                image(Modifier.capturable(captureController))
                Spacer(modifier = Modifier.height(24.dp))
                if (belowImage != null) {
                    belowImage(content)
                    Spacer(modifier = Modifier.height(24.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShareLyricsPill(
                        text = stringResource(Res.string.share_lyrics_save),
                        icon = SimpIcons.Download,
                        container = Color.Transparent,
                        label = content,
                        outlined = true,
                        onClick = {
                            if (!busy) {
                                busy = true
                                // The save itself lives in the permission callback, which always
                                // fires exactly once — so `busy` is cleared on every path.
                                savePermission.requestIfNeeded()
                            }
                        },
                    )
                    ShareLyricsPill(
                        text = stringResource(Res.string.share_lyrics_share_action),
                        icon = SimpIcons.Share,
                        container = content,
                        label = onFilled,
                        onClick = {
                            if (!busy) {
                                busy = true
                                scope.launch {
                                    val bytes = captureController.captureAsync().await().toPngByteArray()
                                    val ok = bytes != null && shareImage(bytes, fileName, title)
                                    if (!ok) showToast(shareFailedMessage, ToastGravity.Bottom)
                                    busy = false
                                }
                            }
                        },
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
