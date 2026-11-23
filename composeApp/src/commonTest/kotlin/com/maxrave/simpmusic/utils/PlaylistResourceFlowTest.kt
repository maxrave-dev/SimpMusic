package com.maxrave.simpmusic.utils

import com.maxrave.domain.utils.LocalResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlaylistResourceFlowTest {
    @Test
    fun zeroEmissionsResolveToErrorInsteadOfStayingLoading() {
        val result = resolvePlaylistFetchOutcome<String>(hasEmitted = false, lastItems = null)
        val error = assertIs<LocalResource.Error<List<String>>>(result)
        assertEquals("Empty response", error.message)
    }

    @Test
    fun nullEmissionResolvesToSuccessWithEmptyList() {
        val result = resolvePlaylistFetchOutcome<String>(hasEmitted = true, lastItems = null)
        val success = assertIs<LocalResource.Success<List<String>>>(result)
        assertEquals(emptyList(), success.data)
    }

    @Test
    fun nonEmptyEmissionPreservesItems() {
        val items = listOf("My Supermix", "My Mix 1")
        val result = resolvePlaylistFetchOutcome(hasEmitted = true, lastItems = items)
        val success = assertIs<LocalResource.Success<List<String>>>(result)
        assertEquals(items, success.data)
    }

    @Test
    fun thrownExceptionResolvesToErrorWithMessage() {
        val result =
            resolvePlaylistFetchOutcome<String>(
                hasEmitted = false,
                lastItems = null,
                failureMessage = "HTTP 401 Unauthorized",
            )
        val error = assertIs<LocalResource.Error<List<String>>>(result)
        assertEquals("HTTP 401 Unauthorized", error.message)
    }
}
