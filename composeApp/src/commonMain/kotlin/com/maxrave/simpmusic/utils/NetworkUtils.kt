package com.maxrave.simpmusic.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

suspend fun isNetworkAvailable(): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            HttpClient(CIO).use { client ->
                val response = client.get("https://dns.google")
                response.status.value in 200..299
            }
        } catch (e: Exception) {
            false
        }
    }
}
