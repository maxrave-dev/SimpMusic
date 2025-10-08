package com.maxrave.ktorext

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun getEngine(): HttpClientEngineFactory<HttpClientEngineConfig> = Darwin