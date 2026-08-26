package com.maxrave.simpmusic.ui.component.lyrics

import com.maxrave.simpmusic.ui.screen.player.content.stripRichSyncTimestamps
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData

/**
 * Flattens lyrics into the plain strings [ShareLyricsSheet] takes.
 *
 * Rich-synced lines carry a `<mm:ss.xx>` marker before every word, so they are stripped here —
 * once, in the one place all four entry points go through, rather than at each of them. The list
 * is NOT compacted: an index into it has to keep meaning the same line the player is counting,
 * because that index is what opens the sheet at the line being sung.
 */
internal fun NowPlayingScreenData.LyricsData.toShareLyricsLines(): List<String> =
    lyrics.lines
        ?.map { it.words.stripRichSyncTimestamps() }
        .orEmpty()
