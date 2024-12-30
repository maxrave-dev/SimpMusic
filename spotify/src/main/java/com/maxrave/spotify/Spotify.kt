package com.maxrave.spotify

import com.maxrave.spotify.model.response.spotify.CanvasResponse
import com.maxrave.spotify.model.response.spotify.PersonalTokenResponse
import com.maxrave.spotify.model.response.spotify.SpotifyLyricsResponse
import com.maxrave.spotify.model.response.spotify.search.SpotifySearchResponse
import io.ktor.client.call.body
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http

class Spotify {
    private val spotifyClient = SpotifyClient()

    /**
     * Remove proxy for client
     */
    fun removeProxy() {
        spotifyClient.proxy = null
    }

    /**
     * Set the proxy for client
     */
    fun setProxy(
        isHttp: Boolean,
        host: String,
        port: Int,
    ) {
        val verifiedHost =
            if (!host.contains("http")) {
                "http://$host"
            } else {
                host
            }
        runCatching {
            if (isHttp) ProxyBuilder.http("$verifiedHost:$port") else ProxyBuilder.socks(verifiedHost, port)
        }.onSuccess {
            spotifyClient.proxy = it
        }.onFailure {
            it.printStackTrace()
        }
    }

    suspend fun getPersonalToken(spdc: String) =
        runCatching {
            spotifyClient.getSpotifyLyricsToken(spdc).body<PersonalTokenResponse>()
        }

    suspend fun searchSpotifyTrack(
        query: String,
        authToken: String,
    ) = runCatching {
        spotifyClient
            .searchSpotifyTrack(query, authToken)
            .body<SpotifySearchResponse>()
    }

    suspend fun getSpotifyLyrics(
        trackId: String,
        token: String,
    ) = runCatching {
        spotifyClient.getSpotifyLyrics(token, trackId).body<SpotifyLyricsResponse>()
    }

    suspend fun getSpotifyCanvas(
        trackId: String,
        token: String,
    ) = runCatching {
        spotifyClient.getSpotifyCanvas(trackId, token).body<CanvasResponse>()
    }
}