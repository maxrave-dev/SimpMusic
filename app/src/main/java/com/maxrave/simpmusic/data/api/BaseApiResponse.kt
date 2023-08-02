package com.maxrave.simpmusic.data.api

import com.maxrave.simpmusic.utils.Resource
import retrofit2.Response

abstract class BaseApiResponse {
    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
        try {
            val response = apiCall()
            if (response.isSuccessful){
                val body = response.body()
                body?.let {
                    return Resource.Success(body)
                }
            }
            return onError(" ${response.code()} ${response.message()}")
        }
        catch (e: Exception){
            return onError(e.message ?: e.toString())
        }
    }
    private fun <T> onError(message: String): Resource<T> {
        return Resource.Error(message, null)
    }
}