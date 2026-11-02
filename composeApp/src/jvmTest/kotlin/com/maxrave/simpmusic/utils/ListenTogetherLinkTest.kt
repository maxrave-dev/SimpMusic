package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.model.listentogether.RoomConnection
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListenTogetherLinkTest {
    @Test
    fun aRoomLinkReadsBackItsOwnCode() {
        assertEquals("ABCD1234", roomCodeFromText(listenTogetherLink("ABCD1234")))
    }

    @Test
    fun acceptsBareCodesInAnyCase() {
        assertEquals("ABCD1234", roomCodeFromText("ABCD1234"))
        assertEquals("ABCD1234", roomCodeFromText("  abcd1234\n"))
    }

    @Test
    fun acceptsTheLinksThatCanCarryARoom() {
        assertEquals("ABCD1234", roomCodeFromText("https://simpmusic.org/app/listen-together?code=abcd1234"))
        assertEquals("ABCD1234", roomCodeFromText("https://www.simpmusic.org/app/listen-together?code=ABCD1234"))
        assertEquals("ABCD1234", roomCodeFromText("https://metrolist.cc/listen?code=ABCD1234"))
    }

    @Test
    fun findsTheLinkInsideTheShareMessage() {
        val message = "Join my SimpMusic room with code ABCD1234\n${listenTogetherLink("ABCD1234")}"
        assertEquals("ABCD1234", roomCodeFromText(message))
    }

    @Test
    fun refusesOtherHostsAndPaths() {
        assertNull(roomCodeFromText("https://simpmusic.org.evil.example/app/listen-together?code=ABCD1234"))
        assertNull(roomCodeFromText("https://evil.example/listen?code=ABCD1234"))
        assertNull(roomCodeFromText("https://simpmusic.org/app/watch?code=ABCD1234"))
        assertNull(roomCodeFromText("http://metrolist.cc/listen?code=ABCD1234"))
        assertNull(roomCodeFromText("simpmusic://watch?code=ABCD1234"))
        assertNull(roomCodeFromText("wordbyword://listen-together?code=ABCD1234"))
    }

    @Test
    fun refusesMissingOrMalformedCodes() {
        assertNull(roomCodeFromText(""))
        assertNull(roomCodeFromText("ABC123"))
        assertNull(roomCodeFromText("ABCD12345"))
        assertNull(roomCodeFromText("ABCD-123"))
        assertNull(roomCodeFromText("simpmusic://listen-together"))
        assertNull(roomCodeFromText("simpmusic://listen-together?code=ABC"))
        assertNull(roomCodeFromText("simpmusic://listen-together?code=ABCD%201234"))
        assertNull(roomCodeFromText("not a uri :// at all"))
    }

    @Test
    fun aBlankNameStopsBothActions() {
        assertEquals(setOf(RoomFormProblem.NAME_MISSING), roomFormProblems(""))
        assertEquals(setOf(RoomFormProblem.NAME_MISSING), roomFormProblems("   "))
        assertEquals(setOf(RoomFormProblem.NAME_MISSING), roomFormProblems(" ", "ABCD1234"))
    }

    @Test
    fun joiningAlsoNeedsAWholeCode() {
        assertEquals(setOf(RoomFormProblem.CODE_INCOMPLETE), roomFormProblems("Cami", "ABC"))
        assertEquals(
            setOf(RoomFormProblem.NAME_MISSING, RoomFormProblem.CODE_INCOMPLETE),
            roomFormProblems("", ""),
        )
    }

    @Test
    fun aCompleteFormHasNoProblems() {
        assertTrue(roomFormProblems("Cami").isEmpty())
        assertTrue(roomFormProblems("Cami", "ABCD1234").isEmpty())
    }

    @Test
    fun aConnectionThatComesUpIsReported() =
        runBlocking {
            assertTrue(flowOf(RoomConnection.Connecting, RoomConnection.Connected("1")).awaitConnected())
            assertTrue(flowOf<RoomConnection>(RoomConnection.Connected("1")).awaitConnected())
        }

    @Test
    fun aStaleFailureBeforeTheAttemptIsSkipped() =
        runBlocking {
            assertTrue(
                flowOf(
                    RoomConnection.Failed("old"),
                    RoomConnection.Disconnected,
                    RoomConnection.Connecting,
                    RoomConnection.Connected("1"),
                ).awaitConnected(),
            )
        }

    @Test
    fun aFailureDuringTheAttemptIsReported() =
        runBlocking {
            assertFalse(flowOf(RoomConnection.Connecting, RoomConnection.Failed("no route")).awaitConnected())
            assertFalse(flowOf(RoomConnection.Connecting, RoomConnection.Disconnected).awaitConnected())
        }
}
