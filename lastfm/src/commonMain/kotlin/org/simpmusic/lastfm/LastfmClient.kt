package org.simpmusic.lastfm

import com.maxrave.ktorext.getEngine
import com.maxrave.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.ByteString.Companion.encodeUtf8

private const val TAG = "LastfmClient"
private const val ENDPOINT = "https://ws.audioscrobbler.com/2.0/"

/**
 * Every call Last.fm needs, signed and parsed.
 *
 * Responses are read as loose [JsonObject]s instead of `@Serializable` classes on purpose. Last.fm's
 * JSON is a mechanical translation of its XML: numbers arrive as strings, attributes hide under
 * `@attr`, and a field is an object with one element but an array with several. A schema that
 * mirrors all of that breaks the moment a response has two entries instead of one, and there is
 * nothing here worth reading past a handful of fields.
 */
internal object LastfmClient {
    private val json = Json { ignoreUnknownKeys = true }

    private val client =
        HttpClient(getEngine()) {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }
        }

    /**
     * Builds `api_sig`.
     *
     * Parameters are ordered by name, concatenated as `<name><value>`, the shared secret is appended
     * and the whole thing is MD5'd. `format` is excluded because the spec says so — signing it is
     * the classic reason every request comes back "Invalid method signature supplied" (code 13).
     */
    private fun sign(
        params: Map<String, String>,
        secret: String,
    ): String =
        params
            .filterKeys { it != "format" && it != "callback" }
            .entries
            // sortedBy, not toSortedMap: the latter is a JDK collection and does not exist in
            // common Kotlin. Parameter names are ASCII, so String ordering is the alphabetical
            // ordering the spec asks for.
            .sortedBy { it.key }
            .joinToString(separator = "") { "${it.key}${it.value}" }
            .plus(secret)
            .encodeUtf8()
            .md5()
            .hex()

    /**
     * Signs [params], posts them as a form and hands back the parsed body.
     *
     * Everything goes over POST, including the two auth calls: the per-method pages call them GET
     * but the authentication spec calls them POST, and POST is accepted for both.
     */
    private suspend fun call(
        params: Map<String, String>,
        secret: String,
    ): JsonObject? =
        runCatching {
            val signed = params + ("api_sig" to sign(params, secret)) + ("format" to "json")
            val body =
                client
                    .submitForm(
                        url = ENDPOINT,
                        formParameters =
                            Parameters.build {
                                signed.forEach { (key, value) -> append(key, value) }
                            },
                    ).bodyAsText()
            json.parseToJsonElement(body).jsonObject
        }.onFailure {
            Logger.e(TAG, "Request failed: ${it.message}")
        }.getOrNull()

    /** Reads a field that Last.fm may send as either a JSON string or a JSON number. */
    private fun JsonObject.intOrNull(key: String): Int? = this[key]?.jsonPrimitive?.content?.toIntOrNull()

    private fun JsonObject.stringOrNull(key: String): String? = this[key]?.jsonPrimitive?.content

    /** Turns an `{"error": N, "message": "..."}` body into an outcome, or null when the call worked. */
    private fun JsonObject.asErrorOrNull(): LastfmOutcome.Error? {
        val code = intOrNull("error") ?: return null
        return LastfmOutcome.Error(code, stringOrNull("message") ?: "Unknown Last.fm error")
    }

    /**
     * Reads the `ignoredMessage` that rides along with an otherwise successful write.
     *
     * Code 0 means nothing was ignored. 1 and 2 mean the artist or track name failed Last.fm's
     * filter, 3 and 4 mean the timestamp was too far in the past or future, 5 means the daily
     * scrobble limit is spent.
     */
    private fun JsonObject.ignoredOrNull(): LastfmOutcome.Ignored? {
        val ignored = this["ignoredMessage"]?.jsonObject ?: return null
        val code = ignored.intOrNull("code") ?: return null
        if (code == 0) return null
        return LastfmOutcome.Ignored(code, ignored.stringOrNull("#text").orEmpty())
    }

    suspend fun getSession(
        apiKey: String,
        secret: String,
        token: String,
    ): LastfmSession? {
        val response =
            call(
                mapOf("method" to "auth.getSession", "api_key" to apiKey, "token" to token),
                secret,
            ) ?: return null
        response.asErrorOrNull()?.let {
            Logger.e(TAG, "auth.getSession failed: ${it.code} ${it.message}")
            return null
        }
        val session = response["session"]?.jsonObject ?: return null
        val name = session.stringOrNull("name") ?: return null
        val key = session.stringOrNull("key") ?: return null
        return LastfmSession(username = name, sessionKey = key)
    }

    suspend fun updateNowPlaying(
        apiKey: String,
        secret: String,
        sessionKey: String,
        track: LastfmTrack,
    ): LastfmOutcome {
        val params =
            buildMap {
                put("method", "track.updateNowPlaying")
                put("api_key", apiKey)
                put("sk", sessionKey)
                put("artist", track.artist)
                put("track", track.track)
                track.album?.takeIf { it.isNotBlank() }?.let { put("album", it) }
                track.albumArtist?.takeIf { it.isNotBlank() }?.let { put("albumArtist", it) }
                track.durationSeconds?.takeIf { it > 0 }?.let { put("duration", it.toString()) }
            }
        val response = call(params, secret) ?: return LastfmOutcome.Error(-1, "Network error")
        response.asErrorOrNull()?.let { return it }
        val nowPlaying = response["nowplaying"]?.jsonObject ?: return LastfmOutcome.Ok
        return nowPlaying.ignoredOrNull() ?: LastfmOutcome.Ok
    }

    /**
     * Sends one play.
     *
     * The parameters are indexed (`artist[0]`) even though a single scrobble may be sent with plain
     * names. Indexing one entry is the same shape a batch uses, so growing this into an offline
     * queue later — Last.fm takes up to 50 per request — costs no rewrite.
     *
     * `duration` is always sent: the method page lists it as optional but the scrobbling guide lists
     * it as required, and sending it satisfies both.
     */
    suspend fun scrobble(
        apiKey: String,
        secret: String,
        sessionKey: String,
        track: LastfmTrack,
        startedAtEpochSeconds: Long,
    ): LastfmOutcome {
        val params =
            buildMap {
                put("method", "track.scrobble")
                put("api_key", apiKey)
                put("sk", sessionKey)
                put("artist[0]", track.artist)
                put("track[0]", track.track)
                put("timestamp[0]", startedAtEpochSeconds.toString())
                track.album?.takeIf { it.isNotBlank() }?.let { put("album[0]", it) }
                track.albumArtist?.takeIf { it.isNotBlank() }?.let { put("albumArtist[0]", it) }
                track.durationSeconds?.takeIf { it > 0 }?.let { put("duration[0]", it.toString()) }
                put("chosenByUser[0]", "1")
            }
        val response = call(params, secret) ?: return LastfmOutcome.Error(-1, "Network error")
        response.asErrorOrNull()?.let { return it }

        val scrobbles = response["scrobbles"]?.jsonObject ?: return LastfmOutcome.Ok
        // One scrobble comes back as an object, several as an array — read whichever arrived.
        val first =
            when (val entry = scrobbles["scrobble"]) {
                is JsonArray -> entry.firstOrNull()?.jsonObject
                is JsonObject -> entry
                else -> null
            }
        return first?.ignoredOrNull() ?: LastfmOutcome.Ok
    }

    suspend fun setLoved(
        apiKey: String,
        secret: String,
        sessionKey: String,
        artist: String,
        track: String,
        loved: Boolean,
    ): LastfmOutcome {
        val params =
            mapOf(
                "method" to if (loved) "track.love" else "track.unlove",
                "api_key" to apiKey,
                "sk" to sessionKey,
                "artist" to artist,
                "track" to track,
            )
        val response = call(params, secret) ?: return LastfmOutcome.Error(-1, "Network error")
        return response.asErrorOrNull() ?: LastfmOutcome.Ok
    }
}
