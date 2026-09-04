package com.maxrave.simpmusic.ui.component.lyrics.accompanist

import com.maxrave.domain.data.model.lyrics.RomanizationLanguage
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.simpmusic.lyrics.romanization.LineScript
import org.simpmusic.lyrics.romanization.LyricsRomanizer
import org.simpmusic.lyrics.romanization.detectScript
import kotlin.math.absoluteValue

object LyricsAdapter {
    private fun parseLrcTimeToMs(time: String): Int {
        val parts = time.split(":")
        val min = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val secParts = parts.getOrNull(1)?.split(".") ?: listOf("0", "0")
        val sec = secParts.getOrNull(0)?.toIntOrNull() ?: 0
        val msStr = secParts.getOrNull(1)?.padEnd(3, '0')?.take(3) ?: "000"
        val ms = msStr.toIntOrNull() ?: 0
        return min * 60000 + sec * 1000 + ms
    }

    private fun buildSyncedTranslatedWordsByLineIndex(
        originalLines: List<com.maxrave.domain.data.model.metadata.Line>,
        translatedLines: List<com.maxrave.domain.data.model.metadata.Line>,
        thresholdMs: Long = 1500L,
    ): Map<Int, String> {
        if (originalLines.isEmpty() || translatedLines.isEmpty()) return emptyMap()
        val sortedTranslated =
            translatedLines
                .mapNotNull { line ->
                    val ts = line.startTimeMs.toLongOrNull() ?: return@mapNotNull null
                    ts to line.words
                }.sortedBy { it.first }

        if (sortedTranslated.isEmpty()) return emptyMap()

        data class OriginalEntry(val index: Int, val ts: Long)

        val sortedOriginal =
            originalLines
                .mapIndexedNotNull { index, line ->
                    val ts = line.startTimeMs.toLongOrNull() ?: return@mapIndexedNotNull null
                    OriginalEntry(index, ts)
                }.sortedBy { it.ts }

        if (sortedOriginal.isEmpty()) return emptyMap()

        val result = HashMap<Int, String>(sortedOriginal.size)
        var j = 0
        for (orig in sortedOriginal) {
            while (j + 1 < sortedTranslated.size && sortedTranslated[j + 1].first <= orig.ts) {
                j++
            }
            val candA = sortedTranslated[j]
            val diffA = kotlin.math.abs(candA.first - orig.ts)
            var bestTs = candA.first
            var bestWords = candA.second
            var bestDiff = diffA
            if (j + 1 < sortedTranslated.size) {
                val candB = sortedTranslated[j + 1]
                val diffB = kotlin.math.abs(candB.first - orig.ts)
                if (diffB < bestDiff) {
                    bestTs = candB.first
                    bestWords = candB.second
                    bestDiff = diffB
                }
            }
            if (bestDiff <= thresholdMs) {
                val origClean = originalLines[orig.index].words.replace(Regex("<[^>]*>"), "").trim()
                val transClean = bestWords.replace(Regex("<[^>]*>"), "").trim()
                if (transClean.isNotEmpty() && !transClean.equals(origClean, ignoreCase = true)) {
                    result[orig.index] = bestWords
                }
            }
        }
        return result
    }

    fun hasNonEnglishLyrics(lyricsData: NowPlayingScreenData.LyricsData?): Boolean {
        if (lyricsData == null) return false
        val lines = lyricsData.lyrics.lines ?: return false
        val originalWords = lines.map { it.words.replace(Regex("<[^>]*>"), "").trim() }.filter { it.isNotEmpty() && it != "♫" }
        val translatedLines = lyricsData.translatedLyrics?.first?.lines
        if (!translatedLines.isNullOrEmpty()) {
            val translatedWords = translatedLines.map { it.words.replace(Regex("<[^>]*>"), "").trim() }.filter { it.isNotEmpty() && it != "♫" }
            if (translatedWords.isNotEmpty() && originalWords.isNotEmpty()) {
                val unchangedCount = originalWords.zip(translatedWords).count { (orig, trans) -> orig.equals(trans, ignoreCase = true) }
                if (unchangedCount.toFloat() / originalWords.size <= 0.75f) {
                    return true
                }
            }
        }
        return lines.any { line ->
            val text = line.words.replace(Regex("<[^>]*>"), "").trim()
            if (text.isEmpty() || text == "♫") return@any false
            val script = detectScript(text)
            if (script != LineScript.LATIN) return@any true
            text.any { c ->
                c in "ñÑ¿¡áéíóúÁÉÍÓÚàèìòùÀÈÌÒÙâêîôûÂÊÎÔÛäëïöüÄËÏÖÜãõÃÕçÇœŒæÆßøØåÅ"
            }
        }
    }

    fun hasRomanizedLyrics(
        lyricsData: NowPlayingScreenData.LyricsData?,
        enabledLanguages: Set<RomanizationLanguage>,
    ): Boolean {
        if (lyricsData == null || enabledLanguages.isEmpty()) return false
        val lines = lyricsData.lyrics.lines ?: return false
        return lines.any { line ->
            val text = line.words.replace(Regex("<[^>]*>"), "").trim()
            if (text.isEmpty() || text == "♫") return@any false
            val script = detectScript(text)
            script != LineScript.LATIN && LyricsRomanizer.romanize(text, enabledLanguages, script) != null
        }
    }

    fun toSyncedLyrics(
        lyricsData: NowPlayingScreenData.LyricsData?,
        enabledLanguages: Set<RomanizationLanguage>? = null,
    ): SyncedLyrics? {
        if (lyricsData == null) return null
        if (lyricsData.lyrics.syncType == "UNSYNCED" || lyricsData.lyrics.syncType == null) return null
        val lines = lyricsData.lyrics.lines ?: return null
        if (lines.isEmpty()) return null
        val hasAnyValidTimestamp = lines.any {
            val ms = it.startTimeMs.toIntOrNull() ?: 0
            ms > 0 || (it.words.contains("<") && it.words.contains(">"))
        }
        if (!hasAnyValidTimestamp) return null

        val activeLanguages = enabledLanguages ?: emptySet()
        val translatedLines = lyricsData.translatedLyrics?.first?.lines.orEmpty()
        val translationByLineIndex = buildSyncedTranslatedWordsByLineIndex(lines, translatedLines)

        val parsedLines = mutableListOf<ISyncedLine>()
        val richSyncRegex = Regex("<(\\d+:\\d{2}\\.\\d{2,3})>([^<]*)")

        for ((index, line) in lines.withIndex()) {
            val startMs = line.startTimeMs.toIntOrNull() ?: (index * 3000)
            val nextStartMs = lines.getOrNull(index + 1)?.startTimeMs?.toIntOrNull()
            val endMsRaw = line.endTimeMs.toIntOrNull() ?: 0
            var endMs = if (endMsRaw > 0) endMsRaw else nextStartMs ?: (startMs + 3000)
            if (endMs < startMs) endMs = startMs + 3000
            val trans = translationByLineIndex[index]

            var words = line.words
            var isBg = words.startsWith("[bg]") || words.startsWith("[BG]")
            if (isBg) words = words.removePrefix("[bg]").removePrefix("[BG]").trim()
            var isV2 = words.startsWith("[v2]") || words.startsWith("[V2]")
            if (isV2) words = words.removePrefix("[v2]").removePrefix("[V2]").trim()

            // Let BiniLyrics parser determine isBg explicitly via [bg] tags.
            // Do not assume parentheses mean background lyrics, as many main lyrics use them.
            val alignment = if (isV2) KaraokeAlignment.End else KaraokeAlignment.Unspecified

            if (words.contains("<") && words.contains(">")) {
                val rawMatches = richSyncRegex.findAll(words).toList()
                val hasTrailingTimestamp = rawMatches.isNotEmpty() && rawMatches.last().groupValues[2].isBlank()
                val syllableMatches = if (hasTrailingTimestamp && rawMatches.size > 1) {
                    rawMatches.dropLast(1)
                } else {
                    rawMatches
                }
                val explicitLineEndMs = if (hasTrailingTimestamp) {
                    parseLrcTimeToMs(rawMatches.last().groupValues[1])
                } else {
                    endMs
                }

                if (syllableMatches.isNotEmpty()) {
                    val fullLineRawText = syllableMatches.joinToString("") { it.groupValues[2] }.trim()
                    val lineScript = detectScript(fullLineRawText)
                    val romanizedLine = LyricsRomanizer.romanize(fullLineRawText, activeLanguages, scriptHint = lineScript)
                    val lineHasNonLatin = romanizedLine != null

                    val syllables = mutableListOf<KaraokeSyllable>()
                    for (i in syllableMatches.indices) {
                        val match = syllableMatches[i]
                        val timeStr = match.groupValues[1]
                        var content = match.groupValues[2]
                        if (isBg) {
                            if (i == 0) content = content.removePrefix("(")
                            if (i == syllableMatches.size - 1) content = content.removeSuffix(")")
                        }
                        val sylStart = parseLrcTimeToMs(timeStr)
                        if (content.isBlank()) {
                            if (syllables.isNotEmpty()) {
                                val prev = syllables.last()
                                syllables[syllables.size - 1] = prev.copy(
                                    content = prev.content + content,
                                    phonetic = prev.phonetic?.let { it + content },
                                    end = sylStart
                                )
                            }
                            continue
                        }
                        val sylEnd = if (i + 1 < syllableMatches.size) {
                            parseLrcTimeToMs(syllableMatches[i + 1].groupValues[1])
                        } else {
                            explicitLineEndMs
                        }
                        var finalSylEnd = sylEnd
                        if (finalSylEnd < sylStart) finalSylEnd = sylStart

                        val trimmedContent = content.trim()
                        val romanizedSyllable = if (lineHasNonLatin) {
                            if (trimmedContent.isNotEmpty()) {
                                val rom = LyricsRomanizer.romanize(trimmedContent, activeLanguages, scriptHint = lineScript)
                                if (rom != null && rom.trim() != trimmedContent) {
                                    // Always append trailing space to romanized syllables so
                                    // groupIntoWords() treats each as a separate word.
                                    if (!rom.endsWith(" ")) "$rom " else rom
                                } else {
                                    null
                                }
                            } else {
                                null
                            }
                        } else {
                            null
                        }

                        val hasSyllablePhonetic = romanizedSyllable != null && romanizedSyllable.trim() != trimmedContent
                        if (hasSyllablePhonetic) {
                            syllables.add(
                                KaraokeSyllable(
                                    content = romanizedSyllable!!,
                                    start = sylStart,
                                    end = finalSylEnd,
                                    phonetic = content
                                )
                            )
                        } else {
                            syllables.add(
                                KaraokeSyllable(
                                    content = content,
                                    start = sylStart,
                                    end = finalSylEnd,
                                    phonetic = if (lineHasNonLatin) content else null
                                )
                            )
                        }
                    }

                    val linePhonetic = if (lineHasNonLatin && romanizedLine?.trim() != fullLineRawText.trim()) fullLineRawText else null

                    if (isBg) {
                        parsedLines.add(
                            KaraokeLine.AccompanimentKaraokeLine(
                                syllables = syllables,
                                translation = trans,
                                phonetic = linePhonetic,
                                alignment = alignment,
                                start = startMs,
                                end = endMs
                            )
                        )
                    } else {
                        parsedLines.add(
                            KaraokeLine.MainKaraokeLine(
                                syllables = syllables,
                                translation = trans,
                                phonetic = linePhonetic,
                                alignment = alignment,
                                start = startMs,
                                end = endMs
                            )
                        )
                    }
                } else {
                    // Contains brackets but no valid timestamps
                    var strippedWords = words.replace(Regex("<[^>]*>"), "").replace("  ", " ").trim()
                    if (isBg) {
                        strippedWords = strippedWords.removePrefix("(").removeSuffix(")").trim()
                    }
                    parsedLines.add(
                        SyncedLine(
                            start = startMs,
                            end = endMs,
                            content = strippedWords.ifEmpty { "♫" },
                            translation = trans,
                        )
                    )
                }
            } else {
                var strippedWords = words.trim()
                if (isBg) {
                    strippedWords = strippedWords.removePrefix("(").removeSuffix(")").trim()
                }
                parsedLines.add(
                    SyncedLine(
                        start = startMs,
                        end = endMs,
                        content = strippedWords.ifEmpty { "♫" },
                        translation = trans,
                    )
                )
            }
        }
        
        // Second pass: attach AccompanimentKaraokeLines to the closest MainKaraokeLine
        val mainLines = parsedLines.filterIsInstance<KaraokeLine.MainKaraokeLine>()
        val bgLines = parsedLines.filterIsInstance<KaraokeLine.AccompanimentKaraokeLine>()
        val unattachedLines = parsedLines.filter { it !is KaraokeLine.AccompanimentKaraokeLine }.toMutableList()
        
        if (mainLines.isNotEmpty() && bgLines.isNotEmpty()) {
            val mainLineMap = mainLines.associateWith { it.accompanimentLines?.toMutableList() ?: mutableListOf() }
            
            for (bgLine in bgLines) {
                // The parent lyrics must start BEFORE or AT THE SAME TIME as the background lyrics (mainLine.start <= bgLine.start).
                // It must never be a future line (mainLine.start > bgLine.start), which would cause premature display under an inactive line.
                val parentCandidates = mainLines.filter { it.start <= bgLine.start }
                val bestParent = parentCandidates.maxByOrNull { it.start }
                
                if (bestParent != null && (bgLine.start - bestParent.start) < 20000) {
                    mainLineMap[bestParent]?.add(bgLine)
                } else {
                    // If no main line started before or at bgLine, keep it as its own separate line
                    val insertIndex = unattachedLines.indexOfFirst { it.start > bgLine.start }
                    if (insertIndex == -1) unattachedLines.add(bgLine)
                    else unattachedLines.add(insertIndex, bgLine)
                }
            }
            
            // Rebuild final lines with updated main lines
            val finalLines = unattachedLines.map { line ->
                if (line is KaraokeLine.MainKaraokeLine) {
                    val accs = mainLineMap[line]
                    if (!accs.isNullOrEmpty()) {
                        line.copy(accompanimentLines = accs)
                    } else {
                        line
                    }
                } else {
                    line
                }
            }
            return SyncedLyrics(lines = finalLines)
        }
        
        return SyncedLyrics(lines = parsedLines)
    }
}
