package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.io.File
import java.io.FilenameFilter

/**
 * Desktop has no system photo picker, so this is the system Open dialog filtered to images — the
 * same AWT FileDialog the backup-restore row in Settings uses (see [showOpenFileDialog]), which is
 * what makes it a native dialog on each desktop OS rather than something drawn in-app.
 *
 * This used to hand back `null` without opening anything, which meant the "change cover" button did
 * nothing at all on Desktop.
 */
@Composable
actual fun photoPickerResult(onResultUri: (String?) -> Unit): PhotoPickerLauncher {
    val currentOnResultUri by rememberUpdatedState(onResultUri)
    return remember {
        object : PhotoPickerLauncher {
            override fun launch() {
                // A plain filesystem path, which is exactly what readLocalImageBytes reads on JVM.
                showOpenFileDialog(imageFileFilter) { file -> currentOnResultUri(file?.path) }
            }
        }
    }
}

// FileDialog's javadoc says filters do nothing on Windows, but OpenJDK's dialog hook there asks
// WFileDialogPeer.checkFilenameFilter about every item (CDN_INCLUDEITEM); macOS and GTK ask too.
private val imageFileFilter =
    FilenameFilter { dir, name ->
        File(dir, name).isDirectory ||
            name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    }
