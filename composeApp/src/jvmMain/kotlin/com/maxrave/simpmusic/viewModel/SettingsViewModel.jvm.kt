package com.maxrave.simpmusic.viewModel

import com.eygraber.uri.Uri
import com.maxrave.common.DB_NAME
import com.maxrave.common.SETTINGS_FILENAME
import com.maxrave.data.io.getHomeFolderPath
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.restore_success
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream

actual suspend fun calculateDataFraction(cacheRepository: CacheRepository): SettingsStorageSectionFraction? = null

actual suspend fun restoreNative(
    commonRepository: CommonRepository,
    uri: Uri,
    getData: () -> Unit,
) {
    ZipInputStream(
        FileInputStream(File(uri.toString())),
    ).use { inputStream ->
        var entry =
            try {
                inputStream.nextEntry
            } catch (e: Exception) {
                null
            }
        while (entry != null) {
            Logger.d("BackupRestore", "Processing entry: ${entry.name}")
            when {
                entry.name == "$SETTINGS_FILENAME.preferences_pb" -> {
                    File(getHomeFolderPath(listOf(".simpmusic")), SETTINGS_FILENAME)
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
            }
            entry = inputStream.nextEntry
        }
        withContext(Dispatchers.Main) {
            showToast(getString(Res.string.restore_success), ToastGravity.Bottom)
        }
    }
}

actual suspend fun backupNative(
    commonRepository: CommonRepository,
    uri: Uri,
    backupDownloaded: Boolean,
) {
    showToast("Not supported in JVM")
}

actual fun getPackageName(): String = ""

actual fun getFileDir(): String = ""

actual fun changeLanguageNative(code: String) {
    Locale.setDefault(
        Locale.forLanguageTag(
            if (code == "id-ID") {
                "in-ID"
            } else {
                code
            },
        ),
    )
}