package com.maxrave.simpmusic.expect

import com.maxrave.domain.data.model.download.DownloadProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceDownloadExportTest {
    @Test
    fun sanitizeDownloadBaseNameStripsSlashesQuotesAndControlChars() {
        val sanitized =
            sanitizeDownloadBaseName(
                title = "Track / \"Live\" \\ Version?\n\t",
                artist = "Artist: <One> | 'Two' `Three`*",
            )

        assertFalse(sanitized.contains('/'))
        assertFalse(sanitized.contains('\\'))
        assertFalse(sanitized.contains('"'))
        assertFalse(sanitized.contains('\''))
        assertFalse(sanitized.contains('`'))
        assertFalse(sanitized.contains('\n'))
        assertFalse(sanitized.contains('\t'))
        assertEquals("Track_Live_Version_-_Artist_One_Two_Three", sanitized)
    }

    @Test
    fun sanitizeDownloadBaseNameFallsBackWhenBlankAndCapsLength() {
        assertEquals(
            "SimpMusic_Track",
            sanitizeDownloadBaseName(title = "///...", artist = "   "),
        )

        val longTitle = "a".repeat(200)
        val capped = sanitizeDownloadBaseName(title = longTitle, artist = "Artist")
        assertTrue(capped.length <= 120)
        assertFalse(capped.startsWith('.'))
        assertFalse(capped.startsWith('_'))
        assertFalse(capped.endsWith('.'))
        assertFalse(capped.endsWith('_'))
    }

    @Test
    fun buildSafeStagingStemContainsOnlyFfmpegSafeCharacters() {
        val stem = buildSafeStagingStem(videoId = "id/with spaces&quotes:'123456789012345678901234567890", timestampMs = 123456789L)
        assertTrue(stem.matches(Regex("""^[A-Za-z0-9_-]+$""")))
        assertTrue(stem.startsWith("stage_"))
        assertTrue(stem.endsWith("_123456789"))
    }

    @Test
    fun normalizeDeviceDownloadProgressHoldsPrematureDoneUntilMergedFileExists() {
        val prematureDone = DownloadProgress(audioDownloadProgress = 1f, isDone = true)
        val normalizedWhileMerging =
            normalizeDeviceDownloadProgress(
                progress = prematureDone,
                requiresFinalMergeExport = true,
                finalMergedOutputExists = false,
            )
        assertFalse(normalizedWhileMerging.isDone)
        assertTrue(normalizedWhileMerging.isMerging)
        assertFalse(normalizedWhileMerging.isError)
        assertEquals(1f, normalizedWhileMerging.audioDownloadProgress)

        val normalizedWhenReady =
            normalizeDeviceDownloadProgress(
                progress = prematureDone,
                requiresFinalMergeExport = true,
                finalMergedOutputExists = true,
            )
        assertTrue(normalizedWhenReady.isDone)
        assertFalse(normalizedWhenReady.isMerging)
    }

    @Test
    fun normalizeDeviceDownloadProgressPassesThroughDesktopAndErrorStates() {
        val desktopDone = DownloadProgress(audioDownloadProgress = 1f, isDone = true)
        assertEquals(
            desktopDone,
            normalizeDeviceDownloadProgress(
                progress = desktopDone,
                requiresFinalMergeExport = false,
                finalMergedOutputExists = false,
            ),
        )

        val errorProgress = DownloadProgress(isDone = true, isError = true, errorMessage = "FFmpeg failed")
        assertEquals(
            errorProgress,
            normalizeDeviceDownloadProgress(
                progress = errorProgress,
                requiresFinalMergeExport = true,
                finalMergedOutputExists = false,
            ),
        )
    }
}
