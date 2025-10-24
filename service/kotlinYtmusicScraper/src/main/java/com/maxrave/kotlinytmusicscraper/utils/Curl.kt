package com.maxrave.kotlinytmusicscraper.utils

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.utils.EmptyContent
import io.ktor.content.ByteArrayContent
import io.ktor.content.TextContent
import io.ktor.http.HeadersBuilder
import io.ktor.http.contentType
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

internal suspend fun generateCurl(
    request: HttpRequestBuilder,
    excludedHeaders: Set<String> = setOf(),
    maskedHeaders: Set<String> = setOf(),
): String =
    coroutineScope {
        buildString {
            append("curl -X ${request.method.value}")

            request.headers.onHeaders(
                excludedHeaders = excludedHeaders,
                maskedHeaders = maskedHeaders,
                onNoContentTypeHeader = {
                    val contentType = request.contentType()?.contentType ?: ""
                    if (contentType.isNotBlank()) append(" -H \"Content-Type: ${contentType}\"")
                },
                onEachHeader = { key, values ->
                    append(" -H \"$key: ${values.joinToString("; ")}\"")
                },
            )

            append(" \"${request.url.buildString()}\"")

            val body = request.body
            val bodyType = request.bodyType?.kotlinType
            if (bodyType != null) {
                val bodyJsonString =
                    try {
                        Json.encodeToString(serializer(bodyType), body)
                    } catch (e: Exception) {
                        body
                    }
                append(" -d '$bodyJsonString'")
            } else {
                append(" -d '$body'")
            }
        }
    }

internal fun HeadersBuilder.onHeaders(
    onEachHeader: (key: String, values: List<String>) -> Unit = { _, _ -> },
    onNoContentTypeHeader: () -> Unit = {},
    excludedHeaders: Set<String> = setOf(),
    maskedHeaders: Set<String> = setOf(),
) {
    val headers = entries()
    if (headers.isEmpty()) {
        onNoContentTypeHeader()
        return
    }

    var containsContentType = false
    headers.filterNot { h -> excludedHeaders.contains(h.key) }.forEach { (key, values) ->
        if (maskedHeaders.contains(key)) {
            onEachHeader(key, listOf("[masked]"))
        } else {
            onEachHeader(key, values)
        }
        if (key == "Content-Type") containsContentType = true
    }
    if (!containsContentType) onNoContentTypeHeader()
}

internal fun Any.onRequestBody(onBodyFound: (String) -> Unit) {
    val body =
        when (this) {
            is TextContent -> {
                val bytes = bytes()
                bytes.decodeToString(0, 0 + bytes.size)
            }

            is ByteArrayContent -> {
                val bytes = bytes()
                bytes.decodeToString(0, 0 + bytes.size)
            }

            is EmptyContent -> ""
            is MultiPartFormDataContent -> "[request body omitted]"
            is String -> this
            else -> toString()
        }
    if (body.isNotBlank()) onBodyFound(body)
}

interface CurlLogger {
    fun log(curl: String)
}

val KtorToCurl =
    createClientPlugin("KtorToCurlPlugin", ::KtorToCurlConfig) {
        val converter = pluginConfig.converter
        val excludedHeaders = pluginConfig.excludedHeaders
        val maskedHeaders = pluginConfig.maskedHeaders
        onRequest { request, _ ->
            val curl = generateCurl(request, excludedHeaders, maskedHeaders)
            if (curl.isNotBlank()) converter.log(curl)
        }
    }

class KtorToCurlConfig {
    var converter: CurlLogger =
        object : CurlLogger {
            override fun log(curl: String) = Unit
        }
    var excludedHeaders: Set<String> = emptySet()
    var maskedHeaders: Set<String> = emptySet()
}