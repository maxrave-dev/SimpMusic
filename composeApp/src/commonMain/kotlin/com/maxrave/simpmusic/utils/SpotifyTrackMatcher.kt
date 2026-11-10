package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.spotify.SpotifyTrackInfo
import kotlin.math.abs

/**
 * Picks the YT Music search result that is really the same recording as a Spotify track.
 *
 * Taking the first search hit is wrong often enough to matter (covers, live takes, sped-up edits,
 * karaoke), so a candidate is only accepted when title, artist AND duration agree. When nothing
 * qualifies the track is reported as unmatched instead of being filled with a guess.
 */
object SpotifyTrackMatcher {
    /** Max length difference, in seconds, between the Spotify track and an accepted candidate. */
    private const val MAX_DURATION_DIFF_SEC = 8

    private const val MIN_TITLE_SIMILARITY = 0.6

    // A result carrying one of these words is a different recording unless the Spotify title has it too.
    private val VERSION_WORDS =
        listOf(
            "live",
            "remix",
            "cover",
            "karaoke",
            "instrumental",
            "sped up",
            "slowed",
            "nightcore",
            "8d",
            "reverb",
            "acoustic",
            "demo",
        )

    // "(feat. X)", "[Remastered]", "- 2011 Remaster": noise that differs between the two catalogues.
    private val BRACKETS = Regex("""[(\[][^)\]]*[)\]]""")
    private val DASH_SUFFIX = Regex("""\s[-–]\s.*$""")

    fun searchQuery(track: SpotifyTrackInfo): String = "${track.title} ${track.artists.firstOrNull().orEmpty()}".trim()

    /** Returns the best acceptable candidate, or null when none is the same song. */
    fun bestMatch(
        track: SpotifyTrackInfo,
        candidates: List<SongsResult>,
    ): SongsResult? {
        val wantedTitle = normalize(stripNoise(track.title))
        if (wantedTitle.isEmpty()) return null
        val wantedArtists = track.artists.map { normalize(it) }.filter { it.isNotEmpty() }
        val wantedSeconds = (track.durationMs / 1000).toInt()

        var best: SongsResult? = null
        var bestScore = 0.0
        for (candidate in candidates) {
            val rawTitle = candidate.title ?: continue
            if (hasForeignVersionWord(track.title, rawTitle)) continue

            val similarity = titleSimilarity(wantedTitle, normalize(stripNoise(rawTitle)))
            if (similarity < MIN_TITLE_SIMILARITY) continue

            val candidateArtists = candidate.artists.orEmpty().map { normalize(it.name) }
            val artistOk =
                wantedArtists.isEmpty() ||
                    wantedArtists.any { wanted ->
                        candidateArtists.any { it.contains(wanted) || wanted.contains(it) }
                    }
            if (!artistOk) continue

            val candidateSeconds = candidate.durationSeconds ?: 0
            val durationDiff =
                if (wantedSeconds > 0 && candidateSeconds > 0) abs(candidateSeconds - wantedSeconds) else null
            if (durationDiff != null && durationDiff > MAX_DURATION_DIFF_SEC) continue

            // Closer title and closer length win; an unknown length ranks below a verified one.
            val score = similarity + (if (durationDiff == null) 0.0 else 1.0 - durationDiff / (MAX_DURATION_DIFF_SEC + 1.0))
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }
        return best
    }

    private fun hasForeignVersionWord(
        spotifyTitle: String,
        candidateTitle: String,
    ): Boolean {
        val wanted = " ${normalize(spotifyTitle)} "
        val got = " ${normalize(candidateTitle)} "
        return VERSION_WORDS.any { word ->
            val padded = " $word "
            got.contains(padded) && !wanted.contains(padded)
        }
    }

    private fun stripNoise(title: String): String = title.replace(BRACKETS, " ").replace(DASH_SUFFIX, "")

    /** Lowercase, letters and digits only, single spaces. Works for any script, not just ASCII. */
    internal fun normalize(text: String): String =
        buildString {
            for (c in text.lowercase()) append(if (c.isLetterOrDigit()) c else ' ')
        }.trim().replace(Regex("""\s+"""), " ")

    private fun titleSimilarity(
        a: String,
        b: String,
    ): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        if (a.contains(b) || b.contains(a)) return 0.9
        val ta = a.split(' ').toSet()
        val tb = b.split(' ').toSet()
        val union = (ta + tb).size
        return if (union == 0) 0.0 else ta.intersect(tb).size.toDouble() / union
    }
}
