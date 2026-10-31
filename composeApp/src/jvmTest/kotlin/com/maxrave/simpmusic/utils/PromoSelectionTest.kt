package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.model.promo.Promo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PromoSelectionTest {
    private val receipt =
        Promo(
            id = "receipt",
            image = mapOf("en" to "https://x/receipt-en.png", "vi" to "https://x/receipt-vi.png"),
            link = "simpmusic://analytics",
            minVersion = "2.3.0",
            requires = listOf("local_tracking"),
        )
    private val wrapped =
        Promo(
            id = "wrapped",
            image = mapOf("en" to "https://x/wrapped-en.png"),
            link = "simpmusic://library?tab=wrapped",
        )

    private fun eligible(
        promos: List<Promo> = listOf(receipt),
        appVersion: String = "2.3.0",
        language: String = "vi",
        localTracking: Boolean = true,
        youTubeLoggedIn: Boolean = true,
        os: String? = null,
    ) = eligiblePromos(promos, appVersion, language, localTracking, youTubeLoggedIn, os)

    @Test
    fun `picks the image for the user's language`() {
        assertEquals("https://x/receipt-vi.png", eligible().single().second)
    }

    @Test
    fun `falls back to the english image`() {
        assertEquals("https://x/receipt-en.png", eligible(language = "fr").single().second)
    }

    @Test
    fun `a blank image for the language falls back to english`() {
        val promo = receipt.copy(image = mapOf("vi" to " ", "en" to "https://x/receipt-en.png"))
        assertEquals("https://x/receipt-en.png", eligible(promos = listOf(promo)).single().second)
    }

    @Test
    fun `skips a banner this version is too old for`() {
        assertEquals(emptyList(), eligible(appVersion = "2.2.0"))
    }

    @Test
    fun `skips when a requirement is not met or not understood`() {
        assertEquals(emptyList(), eligible(localTracking = false))
        assertEquals(emptyList(), eligible(promos = listOf(receipt.copy(requires = listOf("something_new")))))
    }

    @Test
    fun `an entry that names no os shows on every device, as before`() {
        listOf("android", "macos", "windows", "linux", null).forEach { os ->
            assertEquals(listOf(receipt), eligible(os = os).map { it.first }, "os = $os")
        }
    }

    @Test
    fun `an entry limited by os shows on any of the systems it names`() {
        val mac = wrapped.copy(id = "mac", requires = listOf("macos"))
        val pc = wrapped.copy(id = "pc", requires = listOf("windows", "linux"))
        fun ids(os: String?) = eligible(promos = listOf(mac, pc), os = os).map { it.first.id }

        assertEquals(listOf("mac"), ids("macos"))
        assertEquals(listOf("pc"), ids("windows"))
        assertEquals(listOf("pc"), ids("linux"))
        assertEquals(emptyList(), ids("android"))
        assertEquals(emptyList(), ids(null))
    }

    @Test
    fun `an os still needs every other requirement`() {
        val promo = receipt.copy(requires = listOf("android", "local_tracking"))
        assertEquals(listOf(promo), eligible(promos = listOf(promo), os = "android").map { it.first })
        assertEquals(emptyList(), eligible(promos = listOf(promo), os = "android", localTracking = false))
        assertEquals(emptyList(), eligible(promos = listOf(promo), os = "macos"))
    }

    @Test
    fun `a mac gets the macos banner on its turn, other devices skip it`() {
        // A has no os, B is for macOS only: the config's order decides which comes first on a Mac.
        val a = wrapped.copy(id = "a")
        val b = wrapped.copy(id = "b", requires = listOf("macos"))
        fun next(
            os: String,
            stored: String?,
        ) = eligible(promos = listOf(a, b), os = os).firstOrNull { it.first.id !in shownPromoIds(stored, "2.3.0") }?.first?.id

        val afterA = withShownPromo(null, "2.3.0", "a")
        assertEquals("a", next("macos", null))
        assertEquals("b", next("macos", afterA))
        assertEquals("a", next("windows", null))
        assertEquals(null, next("windows", afterA))
    }

    @Test
    fun `names the os the way requires spells it`() {
        assertEquals("android", osRequirementOf("android"))
        assertEquals("macos", osRequirementOf("Mac OS X"))
        assertEquals("windows", osRequirementOf("Windows 11"))
        assertEquals("linux", osRequirementOf("Linux"))
        assertEquals(null, osRequirementOf("FreeBSD"))
    }

    private val lyricsLine =
        LyricsPromo(
            id = "line",
            placement = "lyrics",
            text = mapOf("en" to "Sing it yourself", "vi" to "Tự hát"),
            link = "https://x",
            requires = listOf("macos"),
        )

    private fun line(
        promos: List<LyricsPromo> = listOf(lyricsLine),
        shown: Set<String> = emptySet(),
        language: String = "vi",
        os: String? = "macos",
    ) = lyricsPromo(promos, shown, "2.4.1", language, localTrackingEnabled = true, os = os)

    @Test
    fun `the lyrics line takes the words for the user's language, english otherwise`() {
        assertEquals("Tự hát", line()?.second)
        assertEquals("Sing it yourself", line(language = "fr")?.second)
    }

    @Test
    fun `the lyrics line keeps a banner's rules`() {
        assertEquals(null, line(os = "windows"))
        assertEquals(null, line(shown = setOf("line")))
        assertEquals(null, line(promos = listOf(lyricsLine.copy(placement = null))))
        assertEquals(null, line(promos = listOf(lyricsLine.copy(link = "ftp://x"))))
        assertEquals(null, line(promos = listOf(lyricsLine.copy(text = mapOf("en" to " ")))))
    }

    @Test
    fun `the lyrics line opens only an https page, since an app screen would open behind the player`() {
        assertEquals("line", line(promos = listOf(lyricsLine.copy(link = "HTTPS://x")))?.first?.id)
        assertEquals(null, line(promos = listOf(lyricsLine.copy(link = "simpmusic://analytics"))))
    }

    @Test
    fun `a lyrics entry is never a launch banner, and a banner never a lyrics line`() {
        val block =
            """[{"id":"line","placement":"lyrics","text":{"en":"Sing"},"link":"https://x","requires":["macos"]},""" +
                """{"id":"banner","image":{"en":"u"},"link":"https://x"}]"""
        assertEquals(listOf("line"), decodeLyricsPromos(block).map { it.id })
        assertEquals(listOf("banner"), eligiblePromos(decodePromos(block), "2.4.1", "en", true, true, "macos").map { it.first.id })
    }

    @Test
    fun `skips entries without an id, a link or an image, and keeps the config's order`() {
        val noId = receipt.copy(id = " ")
        val noLink = receipt.copy(link = null)
        val noImage = receipt.copy(image = emptyMap())
        assertEquals(listOf(wrapped, receipt), eligible(promos = listOf(noId, wrapped, noLink, noImage, receipt)).map { it.first })
    }

    @Test
    fun `each banner shows once per version, then the next one has its turn`() {
        val promos = listOf(receipt, wrapped)
        fun next(stored: String?) = eligible(promos = promos).firstOrNull { it.first.id !in shownPromoIds(stored, "2.3.0") }?.first

        assertEquals(receipt, next(null))
        val afterReceipt = withShownPromo(null, "2.3.0", "receipt")
        assertEquals(wrapped, next(afterReceipt))
        val afterBoth = withShownPromo(afterReceipt, "2.3.0", "wrapped")
        assertEquals(null, next(afterBoth))
        // A new version starts over.
        assertEquals(emptySet(), shownPromoIds(afterBoth, "2.3.1"))
        assertEquals(setOf("receipt", "wrapped"), shownPromoIds(afterBoth, "2.3.0"))
    }

    @Test
    fun `compares versions number by number`() {
        assertTrue(isAtLeast("2.10.0", "2.9.0"))
        assertFalse(isAtLeast("2.9.0", "2.10.0"))
        assertTrue(isAtLeast("2.3", "2.3.0"))
        assertTrue(isAtLeast("2.3.0", null))
    }

    @Test
    fun `one malformed entry is dropped alone`() {
        val block =
            """[{"id":"a","image":{"en":"u"},"link":"l","minVersion":2.3},""" +
                """{"id":"b","image":{"en":"u"},"link":"l","requires":"local_tracking"},""" +
                """{"id":"c","image":{"en":"u"},"link":"l"}]"""
        assertEquals(listOf(Promo(id = "c", image = mapOf("en" to "u"), link = "l")), decodePromos(block))
        assertEquals(emptyList<Promo>(), decodePromos("""{"id":"a","image":{"en":"u"}}"""))
    }

    @Test
    fun `only a newer release counts as an update`() {
        assertTrue(isNewerRelease(current = "2.2.0", tag = "v2.3.0"))
        assertFalse(isNewerRelease(current = "2.3.0", tag = "v2.2.0"))
        assertFalse(isNewerRelease(current = "2.3.0", tag = "v2.3.0"))
        assertFalse(isNewerRelease(current = "2.3.0", tag = "2.3.0"))
        assertTrue(isNewerRelease(current = "2.3.0", tag = "2.10.0"))
    }

    @Test
    fun `a hotfix is offered over the release it fixes, and a final release over its beta`() {
        assertTrue(isNewerRelease(current = "1.0.1", tag = "v1.0.1-hf"))
        assertFalse(isNewerRelease(current = "1.0.1-hf", tag = "v1.0.1-hf"))
        assertTrue(isNewerRelease(current = "1.0.0", tag = "v1.0.1-hf"))
        assertTrue(isNewerRelease(current = "0.1.5-beta", tag = "v0.1.5"))
        assertFalse(isNewerRelease(current = "2.3.0", tag = "v2.2.0-hf"))
    }

    @Test
    fun `skips a banner whose link a tap could not open`() {
        val links =
            listOf("http://x", "simpmusic//analytics", "ftp://x", "simpmusic://wrapped", "HTTPS://x", "simpmusic://analytics")
        val promos = links.mapIndexed { i, link -> receipt.copy(id = "p$i", link = link) }
        assertEquals(listOf("HTTPS://x", "simpmusic://analytics"), eligible(promos = promos).map { it.first.link })
    }

    @Test
    fun `hides a banner whose screen this user does not have, with no requires in the config`() {
        fun promo(
            id: String,
            link: String,
        ) = receipt.copy(id = id, link = link, requires = emptyList())
        val all =
            listOf(
                promo("analytics", "simpmusic://analytics"),
                promo("wrapped", "simpmusic://library?tab=wrapped"),
                promo("taste", "simpmusic://taste"),
                promo("youtube", "simpmusic://library?tab=youtube_music_playlist"),
                promo("library", "simpmusic://library?tab=your_library"),
                // A list named by ?type= is what opens, whatever ?tab= says.
                promo("favorite", "simpmusic://library?type=favorite&tab=wrapped"),
            )
        fun ids(
            localTracking: Boolean,
            youTubeLoggedIn: Boolean,
        ) = eligible(promos = all, localTracking = localTracking, youTubeLoggedIn = youTubeLoggedIn).map { it.first.id }

        assertEquals(all.map { it.id }, ids(localTracking = true, youTubeLoggedIn = true))
        assertEquals(listOf("youtube", "library", "favorite"), ids(localTracking = false, youTubeLoggedIn = true))
        assertEquals(listOf("analytics", "wrapped", "taste", "library", "favorite"), ids(localTracking = true, youTubeLoggedIn = false))
    }

    @Test
    fun `an unreadable block decodes to nothing`() {
        assertEquals(emptyList<Promo>(), decodePromos("{not json"))
        assertEquals(emptyList<Promo>(), decodePromos(null))
        assertEquals(
            listOf(Promo(id = "a", image = mapOf("en" to "u"), link = "l")),
            decodePromos("""[{"id":"a","image":{"en":"u"},"link":"l","unknownField":1}]"""),
        )
    }
}
