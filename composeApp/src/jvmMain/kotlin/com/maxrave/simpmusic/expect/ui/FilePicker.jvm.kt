package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.maxrave.data.io.getHomeFolderPath
import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.io.File
import java.io.FilenameFilter
import javax.swing.SwingUtilities

/**
 * Desktop opens files through AWT's FileDialog, which is each OS's own dialog — GetOpenFileName on
 * Windows, NSOpenPanel on macOS, the GTK chooser on Linux — and ships inside the bundled JDK.
 *
 * It replaced Calf's picker, which built its native dialog the moment the launcher was composed, so a
 * failure there crashed every screen that merely held one — Settings, and the local playlist sheet.
 * That failed three ways: Smart App Control blocked the unsigned DLL Calf extracted to
 * ~/.cache/calf-filepicker on Windows, its .so needed GLIBC 2.39 on Linux, and ProGuard's renaming of
 * jodd.net.MimeTypes broke its mime-table lookup on every OS ("Mime types file missing").
 */
@Composable
actual fun filePickerResult(onResultUri: (String?) -> Unit): FilePickerLauncher {
    val currentOnResultUri by rememberUpdatedState(onResultUri)
    return remember {
        object : FilePickerLauncher {
            override fun launch() {
                // A plain path: restoreNative and readPickedFile open it with File(uri.toString()).
                showOpenFileDialog(filter = null) { file -> currentOnResultUri(file?.absolutePath) }
            }
        }
    }
}

/** Shows the system Open dialog and hands back the chosen file, or null when cancelled. */
internal fun showOpenFileDialog(
    filter: FilenameFilter?,
    onResult: (File?) -> Unit,
) {
    // Queued rather than run inside the click handler: the dialog is modal and blocks until closed.
    SwingUtilities.invokeLater {
        val owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow as? Frame
        val dialog = FileDialog(owner, null, FileDialog.LOAD).apply { filenameFilter = filter }
        dialog.isVisible = true
        val chosen = dialog.file?.let { File(dialog.directory, it) }
        dialog.dispose()
        onResult(chosen)
    }
}

@Composable
actual fun fileSaverResult(
    fileName: String,
    mimeType: String,
    onResultUri: (String?) -> Unit,
): FilePickerLauncher =
    object : FilePickerLauncher {
        override fun launch() {
            onResultUri(File(getHomeFolderPath(emptyList()), fileName).absolutePath)
        }
    }
