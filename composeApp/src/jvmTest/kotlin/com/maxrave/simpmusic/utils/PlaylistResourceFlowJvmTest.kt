package com.maxrave.simpmusic.utils

import com.maxrave.domain.utils.LocalResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PlaylistResourceFlowJvmTest {
    @Test
    fun emptyFlowResolvesToErrorWithoutHanging(): Unit =
        runBlocking {
            val updates = mutableListOf<LocalResource<List<String>>>()
            collectPlaylistResource(emptyFlow()) { updates.add(it) }

            assertEquals(1, updates.size)
            assertIs<LocalResource.Error<List<String>>>(updates.single())
        }

    @Test
    fun nullAndNonEmptyFlowsResolveToSuccess(): Unit =
        runBlocking {
            val nullUpdates = mutableListOf<LocalResource<List<String>>>()
            collectPlaylistResource(flowOf<List<String>?>(null)) { nullUpdates.add(it) }
            val emptySuccess = assertIs<LocalResource.Success<List<String>>>(nullUpdates.single())
            assertEquals(emptyList(), emptySuccess.data)

            val valueUpdates = mutableListOf<LocalResource<List<String>>>()
            collectPlaylistResource(flowOf(listOf("Mix A", "Mix B"))) { valueUpdates.add(it) }
            val fullSuccess = assertIs<LocalResource.Success<List<String>>>(valueUpdates.single())
            assertEquals(listOf("Mix A", "Mix B"), fullSuccess.data)
        }

    @Test
    fun throwingFlowResolvesToErrorWhileCancellationPropagates(): Unit =
        runBlocking {
            val errorUpdates = mutableListOf<LocalResource<List<String>>>()
            collectPlaylistResource(
                flow<List<String>?> { throw IllegalStateException("401 Unauthorized") },
            ) { errorUpdates.add(it) }
            val error = assertIs<LocalResource.Error<List<String>>>(errorUpdates.single())
            assertEquals("401 Unauthorized", error.message)

            assertFailsWith<CancellationException> {
                collectPlaylistResource(
                    flow<List<String>?> { throw CancellationException("cancelled") },
                ) {}
            }
        }
}
