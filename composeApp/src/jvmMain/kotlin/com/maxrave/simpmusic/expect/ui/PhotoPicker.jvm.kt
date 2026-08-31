@file:OptIn(ExperimentalCalfApi::class)

package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mohamedrejeb.calf.core.ExperimentalCalfApi
import com.mohamedrejeb.calf.core.LocalPlatformContext
import com.mohamedrejeb.calf.io.getPath
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher

/**
 * Desktop has no system photo picker, so this is Calf's file chooser filtered to images — the same
 * launcher the backup-restore row in Settings already uses, which is what makes it a native dialog
 * on each desktop OS rather than something drawn in-app.
 *
 * This used to hand back `null` without opening anything, which meant the "change cover" button did
 * nothing at all on Desktop.
 */
@Composable
actual fun photoPickerResult(onResultUri: (String?) -> Unit): PhotoPickerLauncher {
    val platformContext = LocalPlatformContext.current
    val launcher =
        rememberFilePickerLauncher(
            type = FilePickerFileType.Image,
            selectionMode = FilePickerSelectionMode.Single,
        ) { files ->
            // A plain filesystem path here, which is exactly what readLocalImageBytes reads on JVM.
            onResultUri(files.firstOrNull()?.getPath(platformContext))
        }
    return remember(launcher) {
        object : PhotoPickerLauncher {
            override fun launch() {
                launcher.launch()
            }
        }
    }
}
