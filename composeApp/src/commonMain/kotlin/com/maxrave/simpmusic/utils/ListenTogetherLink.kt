package com.maxrave.simpmusic.utils

import com.eygraber.uri.Uri
import com.maxrave.domain.data.model.listentogether.RoomConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first

/** The router's path for a room link; also in APP_LINK_PATHS in AppLinks.kt. */
const val LISTEN_TOGETHER_APP_PATH = "listen-together"

/** Room codes are eight characters on metroserver. */
const val ROOM_CODE_LENGTH = 8

private val ROOM_CODE = Regex("[A-Za-z0-9]{$ROOM_CODE_LENGTH}")
private val WHITESPACE = Regex("\\s+")

/**
 * What a room's QR code carries.
 *
 * The custom scheme rather than https://simpmusic.org/app/…: that link only reaches the app once the
 * site serves /.well-known/assetlinks.json without a redirect, and until then a camera app would open
 * a 404 page in the browser. [roomCodeFromText] and the router already accept the https form, so
 * moving to it later changes this one line.
 */
fun listenTogetherLink(roomCode: String): String = "simpmusic://$LISTEN_TOGETHER_APP_PATH?code=$roomCode"

/** A room code in the server's spelling, or null if [code] cannot be one. */
fun normalizedRoomCode(code: String?): String? = code?.trim()?.takeIf { ROOM_CODE.matches(it) }?.uppercase()

/**
 * Finds a room code in text that came from outside the app: a scanned QR code or a paste.
 *
 * Accepts a bare code, our own links, and Metrolist's share link, since rooms are shared between the
 * two apps. Anything else is null. The result is only ever a code to put in the form, so a QR code
 * cannot use this to open a page or join a room by itself.
 */
fun roomCodeFromText(text: String): String? {
    val trimmed = text.trim()
    normalizedRoomCode(trimmed)?.let { return it }
    return trimmed
        .split(WHITESPACE)
        .filter { "://" in it }
        .firstNotNullOfOrNull(::roomCodeFromLink)
}

private fun roomCodeFromLink(link: String): String? {
    val uri = runCatching { Uri.parse(link) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    val host = uri.host?.lowercase()
    val path = uri.pathSegments
    val isRoomLink =
        when {
            scheme == "simpmusic" -> host == LISTEN_TOGETHER_APP_PATH
            scheme != "https" -> false
            host == "simpmusic.org" || host == "www.simpmusic.org" -> path == listOf("app", LISTEN_TOGETHER_APP_PATH)
            host == "metrolist.cc" || host == "www.metrolist.cc" -> path == listOf("listen")
            else -> false
        }
    return if (isRoomLink) normalizedRoomCode(uri.getQueryParameter("code")) else null
}

/** What stops the Listen Together form from going ahead, each shown beside its own field. */
enum class RoomFormProblem {
    NAME_MISSING,
    CODE_INCOMPLETE,
    QR_UNREADABLE,
}

/** Checks the form before creating a room ([roomCode] null) or joining one. */
fun roomFormProblems(
    displayName: String,
    roomCode: String? = null,
): Set<RoomFormProblem> =
    buildSet {
        if (displayName.isBlank()) add(RoomFormProblem.NAME_MISSING)
        if (roomCode != null && normalizedRoomCode(roomCode) == null) add(RoomFormProblem.CODE_INCOMPLETE)
    }

/**
 * Waits for a connection that was just asked for and says whether it came up.
 *
 * Skips the Disconnected or Failed the flow may still hold from before the request: the repository
 * mirrors the session a moment later, and taking that old value as the outcome would drop the action
 * the user is waiting on.
 */
suspend fun Flow<RoomConnection>.awaitConnected(): Boolean =
    dropWhile { it is RoomConnection.Disconnected || it is RoomConnection.Failed }
        .first { it !is RoomConnection.Connecting } is RoomConnection.Connected
