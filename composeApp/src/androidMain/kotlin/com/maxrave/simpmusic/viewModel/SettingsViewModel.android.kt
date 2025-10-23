package com.maxrave.simpmusic.viewModel

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.storage.StorageManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import coil3.imageLoader
import com.eygraber.uri.Uri
import com.eygraber.uri.toAndroidUri
import com.maxrave.common.Config
import com.maxrave.common.DB_NAME
import com.maxrave.common.DOWNLOAD_EXOPLAYER_FOLDER
import com.maxrave.common.EXOPLAYER_DB_NAME
import com.maxrave.common.SETTINGS_FILENAME
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.logger.Logger
import com.maxrave.media3.di.stopService
import com.maxrave.simpmusic.extension.bytesToMB
import com.maxrave.simpmusic.extension.getSizeOfFile
import com.maxrave.simpmusic.extension.zipInputStream
import com.maxrave.simpmusic.extension.zipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform.getKoin
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.restore_success
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

actual suspend fun calculateDataFraction(cacheRepository: CacheRepository): SettingsStorageSectionFraction? {
    val application: Context = getKoin().get()
    return withContext(Dispatchers.Default) {
        val playerCache = cacheRepository.getCacheSize(Config.PLAYER_CACHE)
        val downloadCache = cacheRepository.getCacheSize(Config.DOWNLOAD_CACHE)
        val canvasCache = cacheRepository.getCacheSize(Config.CANVAS_CACHE)
        val mStorageStatsManager =
            application.getSystemService(StorageStatsManager::class.java)
        if (mStorageStatsManager != null) {
            val totalByte =
                mStorageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT).bytesToMB()
            val freeSpace =
                mStorageStatsManager.getFreeBytes(StorageManager.UUID_DEFAULT).bytesToMB()
            val usedSpace = totalByte - freeSpace
            val simpMusicSize = getSizeOfFile(application.filesDir).bytesToMB()
            val thumbSize = (application.imageLoader.diskCache?.size ?: 0L).bytesToMB()
            val otherApp = simpMusicSize.let { usedSpace.minus(it) - thumbSize }
            val databaseSize =
                simpMusicSize - playerCache.bytesToMB() - downloadCache.bytesToMB() - canvasCache.bytesToMB()
            if (totalByte ==
                freeSpace + otherApp + simpMusicSize + thumbSize
            ) {
                SettingsStorageSectionFraction(
                    otherApp = otherApp.toFloat().div(totalByte.toFloat()),
                    downloadCache =
                        downloadCache
                            .bytesToMB()
                            .toFloat()
                            .div(totalByte.toFloat()),
                    playerCache =
                        playerCache
                            .bytesToMB()
                            .toFloat()
                            .div(totalByte.toFloat()),
                    canvasCache =
                        canvasCache
                            .bytesToMB()
                            .toFloat()
                            .div(totalByte.toFloat()),
                    thumbCache = thumbSize.toFloat().div(totalByte.toFloat()),
                    freeSpace = freeSpace.toFloat().div(totalByte.toFloat()),
                    appDatabase = databaseSize.toFloat().div(totalByte.toFloat()),
                )
            } else {
                null
            }
        } else {
            null
        }
    }
}

actual suspend fun restoreNative(
    commonRepository: CommonRepository,
    uri: Uri,
    getData: () -> Unit,
) {
    val application: Context = getKoin().get()
    application.applicationContext.contentResolver.openInputStream(uri.toAndroidUri())?.use {
        it.zipInputStream().use { inputStream ->
            var entry =
                try {
                    inputStream.nextEntry
                } catch (e: Exception) {
                    null
                }

            var downloadFolderCleared = false

            while (entry != null) {
                Logger.d("BackupRestore", "Processing entry: ${entry.name}")
                when {
                    entry.name == "$SETTINGS_FILENAME.preferences_pb" -> {
                        (application.filesDir / "datastore" / "$SETTINGS_FILENAME.preferences_pb")
                            .outputStream()
                            .use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                    }

                    entry.name == DB_NAME -> {
                        runBlocking(Dispatchers.IO) {
                            commonRepository.databaseDaoCheckpoint()
                            commonRepository.closeDatabase()
                        }
                        FileOutputStream(commonRepository.getDatabasePath()).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    entry.name == EXOPLAYER_DB_NAME -> {
                        FileOutputStream(application.getDatabasePath(EXOPLAYER_DB_NAME)).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    entry.name.startsWith("$DOWNLOAD_EXOPLAYER_FOLDER/") -> {
                        Logger.d("BackupRestore", "Found download entry: ${entry.name}")
                        // Clear download folder on first encounter
                        if (!downloadFolderCleared) {
                            val downloadFolder = application.filesDir / DOWNLOAD_EXOPLAYER_FOLDER
                            Logger.d("BackupRestore", "=== RESTORE: Download folder contents BEFORE clearing ===")
                            debugFolderContents(downloadFolder)
                            Logger.d("BackupRestore", "Clearing download folder: ${downloadFolder.absolutePath}")
                            clearFolder(downloadFolder)
                            Logger.d("BackupRestore", "=== RESTORE: Download folder contents AFTER clearing ===")
                            debugFolderContents(downloadFolder)
                            downloadFolderCleared = true
                        }
                        restoreFolder(entry.name, inputStream, "download")
                    }

                    else -> {
                        Logger.d("BackupRestore", "Unhandled entry: ${entry.name}")
                    }
                }
                entry = inputStream.nextEntry
            }
        }
    }
    // Final debug check
    val downloadFolder = application.filesDir / DOWNLOAD_EXOPLAYER_FOLDER
    Logger.d("BackupRestore", "=== RESTORE: Download folder contents AFTER RESTORE ===")
    debugFolderContents(downloadFolder)

    withContext(Dispatchers.Main) {
        showToast(getString(Res.string.restore_success), ToastGravity.Bottom)
//                        mediaPlayerHandler.stopMediaService(application)
        stopService(application)
        getData()
        val ctx = application.applicationContext
        val pm: PackageManager = ctx.packageManager
        val intent = pm.getLaunchIntentForPackage(ctx.packageName)
        val mainIntent = Intent.makeRestartActivityTask(intent?.component)
        ctx.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }
}

private fun backupFolder(
    folder: File,
    baseName: String,
    zipOutputStream: ZipOutputStream,
) {
    if (!folder.exists() || !folder.isDirectory) return

    Logger.d("BackupRestore", "Backing up folder: ${folder.absolutePath} as $baseName")
    folder.listFiles()?.forEach { file ->
        if (file.isFile) {
            val entryName = "$baseName/${file.name}"
            Logger.d("BackupRestore", "Backing up file: $entryName")
            zipOutputStream.putNextEntry(ZipEntry(entryName))
            file.inputStream().buffered().use { inputStream ->
                inputStream.copyTo(zipOutputStream)
            }
            zipOutputStream.closeEntry()
        } else if (file.isDirectory) {
            Logger.d("BackupRestore", "Entering subdirectory: ${file.name}")
            backupFolder(file, "$baseName/${file.name}", zipOutputStream)
        }
    }
}

private fun debugFolderContents(
    folder: File,
    level: Int = 0,
) {
    if (!folder.exists()) {
        Logger.d("BackupRestore", "${"  ".repeat(level)}Folder does not exist: ${folder.absolutePath}")
        return
    }

    Logger.d("BackupRestore", "${"  ".repeat(level)}Folder: ${folder.name} (${folder.absolutePath})")
    folder.listFiles()?.forEach { file ->
        if (file.isFile) {
            Logger.d("BackupRestore", "${"  ".repeat(level + 1)}File: ${file.name} (${file.length()} bytes)")
        } else if (file.isDirectory) {
            debugFolderContents(file, level + 1)
        }
    }
}

private fun clearFolder(folder: File) {
    if (folder.exists() && folder.isDirectory) {
        Logger.d("BackupRestore", "Clearing folder: ${folder.absolutePath}")
        folder.listFiles()?.forEach { file ->
            if (file.isFile) {
                Logger.d("BackupRestore", "Deleting file: ${file.name}")
                file.delete()
            } else if (file.isDirectory) {
                clearFolder(file) // Recursive
                Logger.d("BackupRestore", "Deleting directory: ${file.name}")
                file.delete() // Delete empty directory
            }
        }
    }
}

private fun restoreFolder(
    entryName: String,
    zipInputStream: ZipInputStream,
    baseFolderName: String,
) {
    val application: Context = getKoin().get()
    Logger.d("BackupRestore", "Restoring entry: $entryName")

    // Extract relative path from entry name
    val relativePath = entryName.removePrefix("$baseFolderName/")
    val targetFile = application.filesDir / baseFolderName / relativePath

    Logger.d("BackupRestore", "Target file path: ${targetFile.absolutePath}")
    Logger.d("BackupRestore", "Relative path: $relativePath")

    // Create parent directories if they don't exist
    val parentCreated = targetFile.parentFile?.mkdirs()
    Logger.d("BackupRestore", "Parent dir created: $parentCreated, parent exists: ${targetFile.parentFile?.exists()}")

    try {
        // Restore the file content
        targetFile.outputStream().use { outputStream ->
            val bytesWritten = zipInputStream.copyTo(outputStream)
            Logger.d("BackupRestore", "Restored file: ${targetFile.name}, bytes: $bytesWritten")

            // Verify file was created
            if (targetFile.exists()) {
                Logger.d("BackupRestore", "File exists after restore: ${targetFile.name}, size: ${targetFile.length()}")
            } else {
                Logger.e("BackupRestore", "File NOT created: ${targetFile.name}")
            }
        }
    } catch (e: Exception) {
        Logger.e("BackupRestore", "Error restoring file: ${targetFile.name}")
    }
}

operator fun File.div(child: String): File = File(this, child)

actual suspend fun backupNative(
    commonRepository: CommonRepository,
    uri: Uri,
    backupDownloaded: Boolean,
) {
    val application: Context = getKoin().get()
    application.applicationContext.contentResolver.openOutputStream(uri.toAndroidUri())?.use {
        it.buffered().zipOutputStream().use { outputStream ->
            (application.filesDir / "datastore" / "$SETTINGS_FILENAME.preferences_pb")
                .inputStream()
                .buffered()
                .use { inputStream ->
                    outputStream.putNextEntry(ZipEntry("$SETTINGS_FILENAME.preferences_pb"))
                    inputStream.copyTo(outputStream)
                }
            runBlocking(Dispatchers.IO) {
                commonRepository.databaseDaoCheckpoint()
            }
            FileInputStream(commonRepository.getDatabasePath()).use { inputStream ->
                outputStream.putNextEntry(ZipEntry(DB_NAME))
                inputStream.copyTo(outputStream)
            }
            if (backupDownloaded) {
                (application.getDatabasePath(EXOPLAYER_DB_NAME))
                    .inputStream()
                    .buffered()
                    .use { inputStream ->
                        outputStream.putNextEntry(ZipEntry(EXOPLAYER_DB_NAME))
                        inputStream.copyTo(outputStream)
                    }
                // Backup download folder
                val downloadFolder = application.filesDir / DOWNLOAD_EXOPLAYER_FOLDER
                Logger.d("BackupRestore", "=== BACKUP: Download folder contents BEFORE backup ===")
                debugFolderContents(downloadFolder)
                backupFolder(downloadFolder, DOWNLOAD_EXOPLAYER_FOLDER, outputStream)
            }
        }
    }
}

actual fun getPackageName(): String {
    val application: Context = getKoin().get()
    return application.packageName
}

actual fun getFileDir(): String {
    val application: Context = getKoin().get()
    return application.filesDir.absolutePath
}

actual fun changeLanguageNative(code: String) {
    val localeList =
        LocaleListCompat.forLanguageTags(
            if (code == "id-ID") {
                if (Build.VERSION.SDK_INT >= 35) {
                    "id-ID"
                } else {
                    "in-ID"
                }
            } else {
                code
            },
        )
    Logger.d("Language", localeList.toString())
    AppCompatDelegate.setApplicationLocales(localeList)
}