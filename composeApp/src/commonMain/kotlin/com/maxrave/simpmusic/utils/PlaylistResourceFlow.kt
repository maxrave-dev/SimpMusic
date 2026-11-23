package com.maxrave.simpmusic.utils

import com.maxrave.domain.utils.LocalResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Maps the outcome of collecting a nullable playlist/mix `Flow<List<T>?>` into a terminal
 * [LocalResource] so callers such as `MixForYouScreen` never remain stuck in
 * [LocalResource.Loading] when an upstream repository flow completes with zero emissions or throws
 * (#2944).
 */
fun <T> resolvePlaylistFetchOutcome(
    hasEmitted: Boolean,
    lastItems: List<T>?,
    failureMessage: String? = null,
    emptyStreamErrorMessage: String = "Empty response",
): LocalResource<List<T>> =
    when {
        failureMessage != null -> LocalResource.Error(failureMessage)
        hasEmitted -> LocalResource.Success(lastItems ?: emptyList())
        else -> LocalResource.Error(emptyStreamErrorMessage)
    }

suspend fun <T> collectPlaylistResource(
    source: Flow<List<T>?>,
    emptyStreamErrorMessage: String = "Empty response",
    onUpdate: (LocalResource<List<T>>) -> Unit,
) {
    var hasEmitted = false
    try {
        source.collect { items ->
            hasEmitted = true
            onUpdate(resolvePlaylistFetchOutcome(hasEmitted = true, lastItems = items))
        }
        if (!hasEmitted) {
            onUpdate(
                resolvePlaylistFetchOutcome(
                    hasEmitted = false,
                    lastItems = null,
                    emptyStreamErrorMessage = emptyStreamErrorMessage,
                ),
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        onUpdate(
            resolvePlaylistFetchOutcome(
                hasEmitted = hasEmitted,
                lastItems = null,
                failureMessage = e.message?.takeIf { it.isNotBlank() } ?: emptyStreamErrorMessage,
                emptyStreamErrorMessage = emptyStreamErrorMessage,
            ),
        )
    }
}
