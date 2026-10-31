package com.maxrave.simpmusic.recognition

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.maxrave.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.UUID
import kotlin.random.Random

/** What the recognition sheet shows; [SongRecognizer.recognize] returns one of the last three. */
internal sealed interface Recognition {
    data object Listening : Recognition

    data object Identifying : Recognition

    data class Found(
        val title: String,
        val artist: String,
        val coverArt: String?,
    ) : Recognition

    data object NoMatch : Recognition

    data object Failed : Recognition
}

/**
 * Listens to the microphone, then asks Shazam's unofficial API what is playing. Ported from
 * Metrolist (GPL-3.0), whose recognition is based on Aleksey Saenko's MusicRecognizer
 * (https://github.com/aleksey-saenko/MusicRecognizer).
 */
internal object SongRecognizer {
    private const val TAG = "SongRecognition"

    // 44.1 kHz is the one rate every device must record at; the fingerprint resamples to 16 kHz.
    private const val RECORD_SAMPLE_RATE = 44_100
    private const val RECORD_SECONDS = 12
    private const val MAX_ATTEMPTS = 3

    private val userAgents =
        listOf(
            "Dalvik/2.1.0 (Linux; U; Android 5.0.2; VS980 4G Build/LRX22G)",
            "Dalvik/1.6.0 (Linux; U; Android 4.4.2; SM-T210 Build/KOT49H)",
            "Dalvik/2.1.0 (Linux; U; Android 5.1.1; SM-P905V Build/LMY47X)",
            "Dalvik/2.1.0 (Linux; U; Android 6.0.1; SM-G920F Build/MMB29K)",
            "Dalvik/2.1.0 (Linux; U; Android 5.0; SM-G900F Build/LRX21T)",
        )

    private val timezones =
        listOf("Europe/Paris", "Europe/London", "America/New_York", "America/Los_Angeles", "Asia/Tokyo", "Asia/Dubai")

    /** Records [RECORD_SECONDS] of audio, calls [onIdentifying], then waits for Shazam's answer. */
    suspend fun recognize(onIdentifying: () -> Unit): Recognition =
        try {
            val pcm = record()
            onIdentifying()
            val signature = withContext(Dispatchers.Default) { ShazamSignature.fromPcm(pcm, RECORD_SAMPLE_RATE) }
            ask(signature, sampleMs = pcm.size * 1000L / RECORD_SAMPLE_RATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, "Recognition failed", e)
            Recognition.Failed
        }

    // The sheet starts a pass only once RECORD_AUDIO is granted.
    @SuppressLint("MissingPermission")
    private suspend fun record(): ShortArray =
        withContext(Dispatchers.IO) {
            val format = AudioFormat.ENCODING_PCM_16BIT
            val channel = AudioFormat.CHANNEL_IN_MONO
            val bufferSize = AudioRecord.getMinBufferSize(RECORD_SAMPLE_RATE, channel, format)
            val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, RECORD_SAMPLE_RATE, channel, format, bufferSize)
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                error("Microphone unavailable")
            }
            val pcm = ShortArray(RECORD_SAMPLE_RATE * RECORD_SECONDS)
            var filled = 0
            try {
                recorder.startRecording()
                // Each read blocks for one buffer, so closing the sheet stops the recording promptly.
                while (filled < pcm.size && isActive) {
                    val read = recorder.read(pcm, filled, minOf(bufferSize / 2, pcm.size - filled))
                    if (read < 0) error("AudioRecord.read failed: $read")
                    filled += read
                }
            } finally {
                recorder.stop()
                recorder.release()
            }
            pcm.copyOf(filled)
        }

    private suspend fun ask(
        signature: String,
        sampleMs: Long,
    ): Recognition {
        val client = HttpClient(CIO) { install(HttpTimeout) { requestTimeoutMillis = 30_000 } }
        try {
            repeat(MAX_ATTEMPTS) { attempt ->
                val response =
                    client.post(
                        "https://amp.shazam.com/discovery/v5/en/US/android/-/tag/" +
                            "${UUID.randomUUID().toString().uppercase()}/${UUID.randomUUID()}",
                    ) {
                        parameter("sync", "true")
                        parameter("webv3", "true")
                        parameter("sampling", "true")
                        parameter("connected", "")
                        parameter("shazamapiversion", "v3")
                        parameter("sharehub", "true")
                        parameter("video", "v3")
                        header("User-Agent", userAgents.random())
                        header("Content-Language", "en_US")
                        contentType(ContentType.Application.Json)
                        setBody(requestBody(signature, sampleMs))
                    }
                when {
                    response.status.isSuccess() -> return parse(response.bodyAsText())
                    response.status == HttpStatusCode.NotFound -> {
                        Logger.i(TAG, "No match")
                        return Recognition.NoMatch
                    }
                    response.status == HttpStatusCode.TooManyRequests && attempt < MAX_ATTEMPTS - 1 -> {
                        Logger.w(TAG, "Rate limited, retrying")
                        delay(2_000L shl attempt)
                    }
                    else -> {
                        Logger.w(TAG, "Shazam answered HTTP ${response.status.value}")
                        return Recognition.Failed
                    }
                }
            }
            return Recognition.Failed
        } finally {
            client.close()
        }
    }

    private fun requestBody(
        signature: String,
        sampleMs: Long,
    ): String {
        val now = System.currentTimeMillis() / 1000
        return buildJsonObject {
            // Shazam asks where and when the tag was taken; it gets a random answer, not the user's.
            putJsonObject("geolocation") {
                put("altitude", Random.nextDouble() * 400 + 100)
                put("latitude", Random.nextDouble() * 180 - 90)
                put("longitude", Random.nextDouble() * 360 - 180)
            }
            putJsonObject("signature") {
                put("samplems", sampleMs)
                put("timestamp", now)
                put("uri", signature)
            }
            put("timestamp", now)
            put("timezone", timezones.random())
        }.toString()
    }

    private fun parse(body: String): Recognition {
        val track = (Json.parseToJsonElement(body) as? JsonObject)?.get("track") as? JsonObject
        val title = track?.get("title")?.jsonPrimitive?.contentOrNull
        if (track == null || title.isNullOrBlank()) {
            Logger.i(TAG, "No match")
            return Recognition.NoMatch
        }
        Logger.i(TAG, "Recognized ${track["key"]?.jsonPrimitive?.contentOrNull}")
        val images = track["images"] as? JsonObject
        return Recognition.Found(
            title = title,
            artist = track["subtitle"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            coverArt = (images?.get("coverarthq") ?: images?.get("coverart"))?.jsonPrimitive?.contentOrNull,
        )
    }
}
