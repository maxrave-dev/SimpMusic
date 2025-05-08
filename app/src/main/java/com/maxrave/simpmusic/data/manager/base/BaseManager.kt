package com.maxrave.simpmusic.data.manager.base

import android.content.Context
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.LocalDataSource
import com.maxrave.simpmusic.utils.LocalResource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseManager(
    private val context: Context,
) : KoinComponent {
    protected val dataStoreManager: DataStoreManager by inject()
    protected val localDataSource: LocalDataSource by inject()

    /**
     * Tag for logging
     */
    protected val tag: String = javaClass.simpleName

    protected fun getString(resId: Int): String = context.getString(resId)

    protected suspend fun wrapMessageResource(
        successMessage: String,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend () -> Unit,
    ): Flow<LocalResource<String>> =
        flow {
            emit(LocalResource.Loading())
            runCatching { block.invoke() }
                .onSuccess {
                    emit(LocalResource.Success(successMessage))
                }.onFailure {
                    emit(LocalResource.Error<String>(it.message ?: "Error in wrapBooleanResource"))
                }
        }.flowOn(dispatcher)

    protected suspend fun <T> wrapDataResource(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend () -> T,
    ): Flow<LocalResource<T>> =
        flow {
            emit(LocalResource.Loading())
            runCatching { block.invoke() }
                .onSuccess {
                    emit(LocalResource.Success(it))
                }.onFailure {
                    emit(LocalResource.Error<T>(it.message ?: "Error in wrapDataResource"))
                }
        }.flowOn(dispatcher)

    // For one time emit
    protected suspend fun <T> wrapResultResource(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend () -> Result<T>,
    ): Flow<LocalResource<T>> =
        flow {
            emit(LocalResource.Loading())
            block
                .invoke()
                .onSuccess {
                    emit(LocalResource.Success(it))
                }.onFailure {
                    emit(LocalResource.Error<T>(it.message ?: "Error in wrapResultResource"))
                }
        }.flowOn(dispatcher)

    protected suspend fun forceNoReturn(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend () -> Unit,
    ) = withContext(dispatcher) {
        block.invoke()
    }
}