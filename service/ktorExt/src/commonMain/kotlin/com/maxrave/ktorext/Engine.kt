package com.maxrave.ktorext

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory

expect fun getEngine(): HttpClientEngineFactory<HttpClientEngineConfig>