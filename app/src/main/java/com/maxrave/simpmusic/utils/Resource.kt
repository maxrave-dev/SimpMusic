package com.maxrave.simpmusic.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

// class Resource<out T>(val hstatus: Status, val data: T?, val message: String?) {
//    companion object {
//        fun <T> success(data: T?): Resource<T> {
//            return Resource(Status.SUCCESS, data, null)
//        }
//
//        fun <T> error(msg: String, data: T?): Resource<T> {
//            return Resource(Status.ERROR, data, msg)
//        }
//
//        fun <T> loading(data: T?): Resource<T> {
//            return Resource(Status.LOADING, data, null)
//        }
//    }
// }
//
// enum class Status {
//    SUCCESS,
//    ERROR,
//    LOADING
// }
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
) {
    class Success<T>(
        data: T,
    ) : Resource<T>(data)

    class Error<T>(
        message: String,
        data: T? = null,
    ) : Resource<T>(data, message)
}

/**
 * New resource class that is used to wrap the data that is returned from the manager (only local data sources)
 * 3 state
 * Success, Error, Loading
 */
sealed class LocalResource<T>(
    val data: T? = null,
    val message: String? = null,
) {
    class Success<T>(
        data: T,
    ) : LocalResource<T>(data)

    class Error<T>(
        message: String,
        data: T? = null,
    ) : LocalResource<T>(data, message)

    class Loading<T> : LocalResource<T>()
}

sealed class NoResponseResource(
    val message: String? = null,
) {
    class Success : NoResponseResource()

    class Error(
        message: String,
    ) : NoResponseResource(message)

    class Loading : NoResponseResource()
}

suspend fun <T> Flow<LocalResource<T>>.collectResource(
    distinct: Boolean = true,
    onSuccess: (T?) -> Unit,
    onError: ((String) -> Unit) = {},
    onLoading: (() -> Unit) = {},
) = this.apply { if (distinct) distinctUntilChanged() }.collect { resource ->
    when (resource) {
        is LocalResource.Success -> onSuccess(resource.data)
        is LocalResource.Error -> onError(resource.message ?: "Error in collectResource")
        is LocalResource.Loading -> onLoading()
    }
}

suspend fun Flow<NoResponseResource>.collectNoResponseResource(
    distinct: Boolean = true,
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {},
    onLoading: () -> Unit = {},
) = this.apply { if (distinct) distinctUntilChanged() }.collect { resource ->
    when (resource) {
        is NoResponseResource.Success -> onSuccess()
        is NoResponseResource.Error -> onError(resource.message ?: "Error in collectNoResponseResource")
        is NoResponseResource.Loading -> onLoading()
    }
}

suspend fun <T> Flow<LocalResource<T>>.collectLatestResource(
    distinct: Boolean = true,
    onSuccess: (T?) -> Unit,
    onError: (String) -> Unit = {},
    onLoading: () -> Unit = {},
) = this.apply { if (distinct) distinctUntilChanged() }.collectLatest { resource ->
    when (resource) {
        is LocalResource.Success -> onSuccess(resource.data)
        is LocalResource.Error -> onError(resource.message ?: "Error in collectLatestResource")
        is LocalResource.Loading -> onLoading()
    }
}

suspend fun Flow<NoResponseResource>.collectLatestNoResponseResource(
    distinct: Boolean = true,
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {},
    onLoading: () -> Unit = {},
) = this.apply { if (distinct) distinctUntilChanged() }.collectLatest { resource ->
    when (resource) {
        is NoResponseResource.Success -> onSuccess()
        is NoResponseResource.Error -> onError(resource.message ?: "Error in collectLatestNoResponseResource")
        is NoResponseResource.Loading -> onLoading()
    }
}