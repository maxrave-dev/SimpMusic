package com.simpmusic.lyrics

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlin.math.roundToLong

// Models
data class Syllable(val text: String, val part: Boolean, val timestampMs: Long, val endtimeMs: Long)
data class LyricsLine(
  val text: List<Syllable>,
  val backgroundText: List<Syllable>,
  val timestampMs: Long,
  val endtimeMs: Long,
  val isWordSynced: Boolean = false,
  val alignment: String? = null,
  val songPart: String? = null
)
data class YouLyPlusLyricsResult(val lines: List<LyricsLine>, val source: String? = null)

class LyricsService(private val client: HttpClient) {
  companion object {
    private val KPOE_SERVERS = listOf(
      "https://lyricsplus.prjktla.workers.dev",
      "https://lyrics-plus-backend.vercel.app",
      "https://lyricsplus.onrender.com",
      "https://lyricsplus.prjktla.online"
    )

    private const val DEFAULT_SOURCE = "apple,lyricsplus,musixmatch,spotify,musixmatch-word"

    fun createClient(): HttpClient = HttpClient {
      install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
      // platform-specific engine chosen in Gradle source sets (OkHttp/Darwin/JS/Java etc.)
    }
  }

  suspend fun fetchLyricsFromYouLyPlus(
    title: String,
    artist: String,
    album: String? = null,
    durationMs: Long? = null
  ): YouLyPlusLyricsResult? = withContext(Dispatchers.Default) {
    val t = title.trim()
    val a = artist.trim()
    if (t.isEmpty() || a.isEmpty()) return@withContext null

    val params = mutableMapOf("title" to t, "artist" to a, "source" to DEFAULT_SOURCE)
    album?.takeIf { it.isNotBlank() }?.let { params["album"] = it }
    durationMs?.takeIf { it > 0 }?.let { params["duration"] = ((it.toDouble() / 1000.0).roundToLong()).toString() }

    for (base in KPOE_SERVERS) {
      val url = buildString {
        append(base.trimEnd('/'))
        append("/v2/lyrics/get?")
        append(params.entries.joinToString("&") { (k, v) -> "${k}=${encodeURIComponent(v)}" })
      }
      try {
        val resp: HttpResponse = client.get(url)
        if (resp.status.value in 200..299) {
          val text = resp.bodyAsText()
          val json = Json.parseToJsonElement(text).jsonObject
          val lines = convertKPoeLyrics(json)
          if (!lines.isNullOrEmpty()) {
            val sourceLabel = json["metadata"]?.jsonObject?.get("source")?.jsonPrimitive?.contentOrNull
              ?: json["metadata"]?.jsonObject?.get("provider")?.jsonPrimitive?.contentOrNull
              ?: "LyricsPlus (KPoe)"
            return@withContext YouLyPlusLyricsResult(lines, sourceLabel)
          }
        }
      } catch (e: Exception) {
        // try next server (optionally log)
      }
    }
    return@withContext null
  }

  private fun convertKPoeLyrics(payload: JsonObject): List<LyricsLine>? {
    val rawLyrics = when {
      payload["lyrics"] is JsonArray -> payload["lyrics"]!!.jsonArray
      payload["data"]?.jsonObject?.get("lyrics") is JsonArray -> payload["data"]!!.jsonObject["lyrics"]!!.jsonArray
      payload["data"] is JsonArray -> payload["data"]!!.jsonArray
      else -> null
    } ?: return null
    if (rawLyrics.isEmpty()) return null

    val agents = payload["metadata"]?.jsonObject?.get("agents")?.jsonObject
    val singerAlignmentMap = mutableMapOf<String, String>()
    agents?.entries?.forEachIndexed { idx, (agentKey, agentVal) ->
      val type = agentVal.jsonObject["type"]?.jsonPrimitive?.contentOrNull
      when (type) {
        "group" -> singerAlignmentMap[agentKey] = "start"
        "other" -> singerAlignmentMap[agentKey] = "end"
        "person" -> singerAlignmentMap[agentKey] = if (idx % 2 == 0) "start" else "end"
      }
    }

    val lines = mutableListOf<LyricsLine>()
    for (entryElem in rawLyrics) {
      val entryObj = entryElem as? JsonObject ?: continue

      val lineStart = toMilliseconds(entryObj["time"] ?: entryObj["startTime"], 0L)
      val lineDuration = toMilliseconds(entryObj["duration"], 0L)
      val explicitEnd = toMilliseconds(entryObj["endTime"], 0L)
      val lineEnd = if (explicitEnd > 0L) explicitEnd else lineStart + (if (lineDuration > 0L) lineDuration else 0L)

      val syllabusArray = (entryObj["syllabus"] as? JsonArray)?.filterIsInstance<JsonElement>() ?: emptyList()
      val mainSyllables = mutableListOf<Syllable>()
      val backgroundSyllables = mutableListOf<Syllable>()

      for (sylElem in syllabusArray) {
        val syl = sylElem as? JsonObject ?: continue
        val sylStart = toMilliseconds(syl["time"], lineStart)
        val sylDuration = toMilliseconds(syl["duration"], 0L)
        val sylEnd = if (sylDuration > 0L) sylStart + sylDuration else lineEnd
        val text = syl["text"]?.jsonPrimitive?.contentOrNull ?: ""
        val part = syl["part"]?.jsonPrimitive?.booleanOrNull ?: false
        val isBackground = syl["isBackground"]?.jsonPrimitive?.booleanOrNull ?: false
        val s = Syllable(text = text, part = part, timestampMs = sylStart, endtimeMs = sylEnd)
        if (isBackground) backgroundSyllables.add(s) else mainSyllables.add(s)
      }

      val lineTextCandidate = entryObj["text"]?.jsonPrimitive?.contentOrNull ?: ""
      if (mainSyllables.isEmpty() && lineTextCandidate.isNotBlank()) {
        mainSyllables.add(Syllable(lineTextCandidate, part = false, timestampMs = lineStart, endtimeMs = lineEnd))
      }

      val hasWordSync = mainSyllables.isNotEmpty() || backgroundSyllables.isNotEmpty()

      val alignment = run {
        val elementVal = entryObj["element"]
        when (elementVal) {
          is JsonArray -> {
            val elements = elementVal.mapNotNull { it.jsonPrimitive.contentOrNull }
            if (elements.contains("opposite") || elements.contains("right")) "end" else null
          }
          is JsonPrimitive -> elementVal.contentOrNull
          else -> null
        }
      }

      val songPartCandidate = (entryObj["element"]?.jsonPrimitive?.contentOrNull) ?: entryObj["element"]?.toString()
      val line = LyricsLine(
        text = mainSyllables,
        backgroundText = backgroundSyllables,
        timestampMs = lineStart,
        endtimeMs = lineEnd,
        isWordSynced = hasWordSync,
        alignment = alignment,
        songPart = songPartCandidate
      )
      lines.add(line)
    }
    return if (lines.isNotEmpty()) lines else null
  }

  private fun toMilliseconds(elem: JsonElement?, fallback: Long): Long {
    if (elem == null) return fallback
    val prim = elem.jsonPrimitive
    prim.longOrNull?.let { v ->
      // If large number likely ms; else keep as-is (payload varies). Use heuristics if needed.
      return v
    }
    prim.doubleOrNull?.let { d ->
      return if (d % 1.0 != 0.0) (d * 1000.0).roundToLong() else d.toLong()
    }
    val s = prim.contentOrNull ?: return fallback
    return try {
      if (s.contains('.')) (s.toDouble() * 1000.0).roundToLong() else s.toLong()
    } catch (_: Exception) {
      fallback
    }
  }

  private fun encodeURIComponent(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")
}
