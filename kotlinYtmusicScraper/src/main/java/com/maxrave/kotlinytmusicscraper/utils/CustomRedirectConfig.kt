package com.maxrave.kotlinytmusicscraper.utils

/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.Sender
import io.ktor.client.plugins.plugin
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.events.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.logging.*
import kotlin.native.concurrent.*

private val ALLOWED_FOR_REDIRECT: Set<HttpMethod> = setOf(HttpMethod.Get, HttpMethod.Head)

private val LOGGER = KtorSimpleLogger("io.ktor.client.plugins.HttpRedirect")

/**
 * An [HttpClient] plugin that handles HTTP redirects
 * Use only for Musixmatch API
 * @author maxrave-dev
 */
class CustomRedirectConfig private constructor(
    private val checkHttpMethod: Boolean,
    private val allowHttpsDowngrade: Boolean,
    private val defaultHostUrl : String? = null
) {

    @KtorDsl
    class Config {

        /**
         * Checks whether the HTTP method is allowed for the redirect.
         * Only [HttpMethod.Get] and [HttpMethod.Head] are allowed for implicit redirection.
         *
         * Please note: changing this flag could lead to security issues, consider changing the request URL instead.
         */
        var checkHttpMethod: Boolean = true

        /**
         * `true` allows a client to make a redirect with downgrading from HTTPS to plain HTTP.
         */
        var allowHttpsDowngrade: Boolean = false

        var defaultHostUrl: String? = null
    }

    companion object Plugin : HttpClientPlugin<Config, CustomRedirectConfig> {
        override val key: AttributeKey<CustomRedirectConfig> = AttributeKey("HttpRedirect")

        /**
         * Occurs when receiving a response with a redirect message.
         */
        val HttpResponseRedirect: EventDefinition<HttpResponse> = EventDefinition()

        override fun prepare(block: Config.() -> Unit): CustomRedirectConfig {
            val config = Config().apply(block)
            return CustomRedirectConfig(
                checkHttpMethod = config.checkHttpMethod,
                allowHttpsDowngrade = config.allowHttpsDowngrade,
                defaultHostUrl = config.defaultHostUrl
            )
        }

        override fun install(plugin: CustomRedirectConfig, scope: HttpClient) {
            scope.plugin(HttpSend).intercept { context ->
                val origin = execute(context)
                if (plugin.checkHttpMethod && origin.request.method !in ALLOWED_FOR_REDIRECT) {
                    return@intercept origin
                }

                handleCall(context, origin, plugin.allowHttpsDowngrade, scope, plugin.defaultHostUrl)
            }
        }

        @OptIn(InternalAPI::class)
        private suspend fun Sender.handleCall(
            context: HttpRequestBuilder,
            origin: HttpClientCall,
            allowHttpsDowngrade: Boolean,
            client: HttpClient,
            defaultHostUrl: String?,
        ): HttpClientCall {
            if (!origin.response.status.isRedirect()) return origin

            var call = origin
            var requestBuilder = context
            val originProtocol = origin.request.url.protocol
            val originAuthority = origin.request.url.authority

            while (true) {
                client.monitor.raise(HttpResponseRedirect, call.response)

                var location = call.response.headers[HttpHeaders.Location]
                LOGGER.trace("Received redirect response to $location for request ${context.url}")
                println("Location header: $location")

                requestBuilder = HttpRequestBuilder().apply {
                    takeFromWithExecutionContext(requestBuilder)
                    url.parameters.clear()

                    if (defaultHostUrl!= null) {
                        if (location?.contains(defaultHostUrl) == false) {
                            LOGGER.trace("Adding default host url to redirect location")
                            location = defaultHostUrl + location
                            println("New redirect URL: $location")
                        }
                    }

                    location?.let { url.takeFrom(it) }

                    /**
                     * Disallow redirect with a security downgrade.
                     */
                    if (!allowHttpsDowngrade && originProtocol.isSecure() && !url.protocol.isSecure()) {
                        LOGGER.trace("Can not redirect ${context.url} because of security downgrade")
                        return call
                    }

                    if (originAuthority != url.authority) {
                        headers.remove(HttpHeaders.Authorization)
                        LOGGER.trace("Removing Authorization header from redirect for ${context.url}")
                    }
                }

                call = execute(requestBuilder)
                if (!call.response.status.isRedirect()) return call
            }
        }
    }
}

private fun HttpStatusCode.isRedirect(): Boolean = when (value) {
    HttpStatusCode.MovedPermanently.value,
    HttpStatusCode.Found.value,
    HttpStatusCode.TemporaryRedirect.value,
    HttpStatusCode.PermanentRedirect.value,
    HttpStatusCode.SeeOther.value -> true
    else -> false
}
