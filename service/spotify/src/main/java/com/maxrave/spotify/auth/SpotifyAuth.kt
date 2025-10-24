package com.maxrave.spotify.auth

import com.maxrave.spotify.SpotifyClient
import com.maxrave.spotify.model.response.spotify.PersonalTokenResponse
import io.ktor.client.call.body
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Handles advanced Spotify authentication with TOTP
 */
class SpotifyAuth(
    private val spotifyClient: SpotifyClient,
) {
    var totpSecret: Pair<Int, List<Int>>? = null

    suspend fun getTotpSecret(): Result<Boolean> =
        runCatching {
            val response = spotifyClient.getSpotifyLastestTotpSecret().body<Map<String, List<Int>>>()
            if (response.isNotEmpty()) {
                val latestDict = response.entries.last()
                val firstKey = latestDict.key.toInt()
                totpSecret = Pair(
                    firstKey,
                    latestDict.value
                )
                return@runCatching true
            } else false
        }

    /**
     * Refresh Spotify token using the new TOTP-based authentication
     * This is more reliable than the standard token method
     */
    suspend fun refreshToken(spDc: String): Result<PersonalTokenResponse> =
        runCatching {
            if (totpSecret == null) {
                // Fetch the latest TOTP secret if not already fetched
                getTotpSecret().onSuccess {
                    println("Fetched TOTP secret successfully: $totpSecret")
                }.onFailure {
                    println("Failed to fetch TOTP secret: ${it.message}")
                }
            }
            // Get server time from Spotify
            val serverTimeResponse = spotifyClient.getSpotifyServerTime(spDc)
            val serverTimeJson = Json.parseToJsonElement(serverTimeResponse.body<String>()).jsonObject
            val serverTime =
                serverTimeJson["serverTime"]?.jsonPrimitive?.longOrNull
                    ?: throw Exception("Failed to get server time")
            println("Server time: $serverTime")

            // Generate TOTP secret and OTP value
            val otpValue = SpotifyTotp.at(serverTime * 1000L, totpSecret ?: SpotifyTotp.TOTP_SECRET_V22)
            println("Generated OTP: $otpValue")

            val sTime = "$serverTime"
            val cTime = "$serverTime"

            // First try with transport mode
            var response =
                try {
                    spotifyClient.getSpotifyAccessToken(
                        spdc = spDc,
                        otpValue = otpValue,
                        reason = "transport",
                        sTime = sTime,
                        cTime = cTime,
                        totpVersion = totpSecret?.first ?: SpotifyTotp.TOTP_SECRET_V22.first,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

            var tokenData =
                try {
                    response?.body<PersonalTokenResponse>()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

            // Check if token is valid (should be 374 characters)
            if (tokenData?.accessToken?.length != 374) {
                // Retry with init mode
                response =
                    spotifyClient.getSpotifyAccessToken(
                        spdc = spDc,
                        otpValue = otpValue,
                        reason = "init",
                        sTime = sTime,
                        cTime = cTime,
                        totpVersion = totpSecret?.first ?: SpotifyTotp.TOTP_SECRET_V22.first,
                    )
                tokenData = response.body<PersonalTokenResponse>()
            }

            // Validate token
            if (tokenData.accessToken.isEmpty()) {
                throw Exception("Unsuccessful token request")
            }

            tokenData
        }
}