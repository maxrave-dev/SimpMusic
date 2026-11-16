@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.maxrave.simpmusic.viewModel

import com.eygraber.uri.Uri
import com.maxrave.common.DB_NAME
import com.maxrave.common.SETTINGS_FILENAME
import com.maxrave.data.db.documentDirectory
import com.maxrave.data.io.fileDir
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.simpmusic.expect.topViewController
import kotlinx.coroutines.IO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.*
import okio.Path.Companion.toPath
import platform.Foundation.*
import platform.UIKit.*

private val fs = FileSystem.SYSTEM
private val settingsName = "$SETTINGS_FILENAME.preferences_pb"
private val pendingRoot get() = (fileDir() + "/pending-restore").toPath()
private fun path(uri: Uri) = (NSURL.URLWithString(uri.toString())?.path ?: uri.toString()).toPath()

actual suspend fun calculateDataFraction(cacheRepository: CacheRepository): SettingsStorageSectionFraction? = null
actual suspend fun readPickedFile(uri: Uri): ByteArray = withContext(Dispatchers.IO) { fs.read(path(uri)) { readByteArray() } }
actual fun getPackageName(): String = "com.maxrave.simpmusic"
actual fun getFileDir(): String = fileDir()
actual fun changeLanguageNative(code: String) {
    NSUserDefaults.standardUserDefaults.setObject(listOf(code), forKey = "AppleLanguages")
}

/** Applied before Koin opens Room/DataStore, so no live database or preferences are overwritten. */
internal fun applyPendingIosRestore() {
    val root = pendingRoot
    if (!fs.exists(root / "ready")) return
    for (name in listOf(DB_NAME, settingsName)) {
        val source = root / name
        if (fs.exists(source)) {
            val target = documentDirectory().toPath() / name
            if (name == DB_NAME) {
                fs.delete((target.toString() + "-wal").toPath(), mustExist = false)
                fs.delete((target.toString() + "-shm").toPath(), mustExist = false)
            }
            fs.atomicMove(source, target)
        }
    }
    val downloads = root / "downloads"
    if (fs.exists(downloads)) {
        val target = (fileDir() + "/downloads").toPath()
        fs.createDirectories(target)
        fs.list(downloads).forEach { fs.atomicMove(it, target / it.name) }
    }
    fs.deleteRecursively(root)
}

actual suspend fun restoreNative(commonRepository: CommonRepository, uri: Uri, getData: () -> Unit) {
    val root = pendingRoot
    fs.deleteRecursively(root, mustExist = false)
    fs.createDirectories(root)
    try {
        fs.openZip(path(uri)).use { zip ->
            val archiveRoot = "/".toPath()
            require(zip.exists(archiveRoot / DB_NAME)) { "Backup contains no music database" }
            for (name in listOf(DB_NAME, settingsName)) {
                val source = archiveRoot / name
                if (zip.exists(source)) zip.source(source).use { input -> fs.sink(root / name).buffer().use { it.writeAll(input) } }
            }
            val downloads = archiveRoot / "downloads"
            if (zip.exists(downloads)) {
                fs.createDirectories(root / "downloads")
                zip.list(downloads).filter { it.name.endsWith(".m4a") && zip.metadata(it).isRegularFile }.forEach { source ->
                    zip.source(source).use { input -> fs.sink(root / "downloads" / source.name).buffer().use { it.writeAll(input) } }
                }
            }
        }
        fs.write(root / "ready") { writeUtf8("ready") }
    } catch (error: Throwable) {
        fs.deleteRecursively(root, mustExist = false)
        throw error
    }
    withContext(Dispatchers.Main) {
        val alert = UIAlertController.alertControllerWithTitle("Backup ready", "Close SimpMusic from the app switcher and reopen it to apply the restored library.", UIAlertControllerStyleAlert)
        alert.addAction(UIAlertAction.actionWithTitle("OK", UIAlertActionStyleDefault, null))
        topViewController()?.presentViewController(alert, true, null)
    }
}

private data class ZipEntry(val name: ByteArray, val crc: Int, val size: Long, val offset: Long)
private fun crc32(path: Path): Int {
    var crc = -1
    fs.source(path).buffer().use { input ->
        while (!input.exhausted()) {
            crc = crc xor (input.readByte().toInt() and 255)
            repeat(8) { crc = (crc ushr 1) xor (if (crc and 1 != 0) 0xedb88320.toInt() else 0) }
        }
    }
    return crc.inv()
}

actual suspend fun backupNative(commonRepository: CommonRepository, uri: Uri, backupDownloaded: Boolean) {
    commonRepository.databaseDaoCheckpoint()
    val entries = mutableListOf(DB_NAME to requireNotNull(commonRepository.getDatabasePath()).toPath(), settingsName to (documentDirectory().toPath() / settingsName))
    val downloadRoot = (fileDir() + "/downloads").toPath()
    if (backupDownloaded) fs.listOrNull(downloadRoot).orEmpty().filter { it.name.endsWith(".m4a") }.forEach { entries += "downloads/${it.name}" to it }
    // Stored ZIP entries keep exports compatible with desktop/Android without a Native ZIP dependency.
    fs.sink(path(uri)).buffer().use { out ->
        val index = mutableListOf<ZipEntry>()
        var written = 0L
        for ((name, file) in entries) {
            if (!fs.exists(file)) continue
            val bytes = name.encodeToByteArray()
            val size = fs.metadata(file).size ?: 0L
            require(size <= 0xffffffffL) { "Backup entry is too large" }
            val crc = crc32(file)
            index += ZipEntry(bytes, crc, size, written)
            out.writeIntLe(0x04034b50).writeShortLe(20).writeShortLe(0x800).writeShortLe(0)
                .writeShortLe(0).writeShortLe(0).writeIntLe(crc).writeIntLe(size.toInt()).writeIntLe(size.toInt())
                .writeShortLe(bytes.size).writeShortLe(0).write(bytes)
            fs.source(file).use { out.writeAll(it) }
            written += 30 + bytes.size + size
        }
        val centralStart = written
        for (entry in index) {
            with(entry) {
                out.writeIntLe(0x02014b50).writeShortLe(20).writeShortLe(20).writeShortLe(0x800).writeShortLe(0)
                    .writeShortLe(0).writeShortLe(0).writeIntLe(crc).writeIntLe(size.toInt()).writeIntLe(size.toInt())
                    .writeShortLe(name.size).writeShortLe(0).writeShortLe(0).writeShortLe(0).writeShortLe(0)
                    .writeIntLe(0).writeIntLe(offset.toInt()).write(name)
                written += 46 + entry.name.size
            }
        }
        out.writeIntLe(0x06054b50).writeShortLe(0).writeShortLe(0).writeShortLe(index.size).writeShortLe(index.size)
            .writeIntLe((written - centralStart).toInt()).writeIntLe(centralStart.toInt()).writeShortLe(0)
    }
    withContext(Dispatchers.Main) {
        val picker = UIDocumentPickerViewController(uRLs = listOf(NSURL.fileURLWithPath(path(uri).toString())), inMode = UIDocumentPickerMode.UIDocumentPickerModeExportToService)
        topViewController()?.presentViewController(picker, true, null)
    }
}
