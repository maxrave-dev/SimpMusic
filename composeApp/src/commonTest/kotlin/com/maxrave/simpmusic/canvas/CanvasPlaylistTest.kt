package com.maxrave.simpmusic.canvas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanvasPlaylistTest {
    private val playlistUrl = "https://audio.example.test/artwork/master/variant.m3u8"

    @Test
    fun relativeSegmentsBecomeLocalFiles() {
        val rewritten =
            rewriteCanvasPlaylist(
                """
                #EXTM3U
                #EXT-X-VERSION:7
                #EXT-X-TARGETDURATION:4
                #EXT-X-MAP:URI="init.mp4",BYTERANGE="720@0"
                #EXTINF:4.0,
                #EXT-X-BYTERANGE:1000@720
                segment-0.mp4
                #EXTINF:4.0,
                segment-1.mp4
                #EXT-X-ENDLIST
                """.trimIndent(),
                playlistUrl,
            )

        assertTrue(rewritten.text.contains("URI=\"part-0.mp4\",BYTERANGE=\"720@0\""))
        assertTrue(rewritten.text.contains("\npart-1.mp4\n"))
        assertTrue(rewritten.text.contains("\npart-2.mp4\n"))
        assertEquals(
            listOf(
                "https://audio.example.test/artwork/master/init.mp4",
                "https://audio.example.test/artwork/master/segment-0.mp4",
                "https://audio.example.test/artwork/master/segment-1.mp4",
            ),
            rewritten.entries.map { it.sourceUrl },
        )
        assertEquals(
            listOf("part-0.mp4", "part-1.mp4", "part-2.mp4"),
            rewritten.entries.map { it.fileName },
        )
    }

    @Test
    fun duplicateUrisShareOneFile() {
        val rewritten =
            rewriteCanvasPlaylist(
                """
                #EXTM3U
                #EXT-X-MAP:URI="media.mp4",BYTERANGE="100@0"
                #EXTINF:4.0,
                #EXT-X-BYTERANGE:900@100
                media.mp4
                #EXTINF:4.0,
                #EXT-X-BYTERANGE:900@1000
                media.mp4
                #EXT-X-ENDLIST
                """.trimIndent(),
                playlistUrl,
            )

        assertEquals(1, rewritten.entries.size)
        assertEquals("part-0.mp4", rewritten.entries.single().fileName)
        assertEquals(
            2,
            Regex("^part-0\\.mp4$", RegexOption.MULTILINE).findAll(rewritten.text).count(),
        )
    }

    @Test
    fun keyUrisAreDownloadedAndRewritten() {
        val rewritten =
            rewriteCanvasPlaylist(
                """
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,URI="key.bin"
                #EXTINF:4.0,
                segment.ts
                #EXT-X-ENDLIST
                """.trimIndent(),
                playlistUrl,
            )

        assertTrue(rewritten.text.contains("URI=\"part-0.bin\""))
        assertTrue(rewritten.text.contains("\npart-1.ts\n"))
        assertEquals(
            listOf(
                "https://audio.example.test/artwork/master/key.bin",
                "https://audio.example.test/artwork/master/segment.ts",
            ),
            rewritten.entries.map { it.sourceUrl },
        )
    }

    @Test
    fun absoluteUrisStayAbsoluteInTheDownloadList() {
        val rewritten =
            rewriteCanvasPlaylist(
                """
                #EXTM3U
                #EXTINF:4.0,
                https://cdn.example.test/segment.ts
                #EXT-X-ENDLIST
                """.trimIndent(),
                playlistUrl,
            )

        assertEquals("https://cdn.example.test/segment.ts", rewritten.entries.single().sourceUrl)
        assertTrue(rewritten.text.contains("\npart-0.ts\n"))
    }

    @Test
    fun masterPlaylistIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            rewriteCanvasPlaylist(
                """
                #EXTM3U
                #EXT-X-STREAM-INF:BANDWIDTH=1000,RESOLUTION=100x100,CODECS="avc1.4d401f"
                variant.m3u8
                """.trimIndent(),
                playlistUrl,
            )
        }
    }

    @Test
    fun unrelatedTagsAndCarriageReturnsArePreserved() {
        val rewritten =
            rewriteCanvasPlaylist(
                "#EXTM3U\r\n#EXT-X-VERSION:7\r\n#EXTINF:4.0,\r\nseg.mp4\r\n#EXT-X-ENDLIST\r\n",
                playlistUrl,
            )

        assertTrue(rewritten.text.contains("#EXT-X-VERSION:7"))
        assertTrue(rewritten.text.contains("#EXT-X-ENDLIST"))
        assertFalse(rewritten.text.contains('\r'))
        assertTrue(rewritten.text.contains("\npart-0.mp4\n"))
    }
}
